// Initialize the application when the DOM is fully loaded
document.addEventListener('DOMContentLoaded', main);

/**
 * Main initialization function that injects metadata and sets up list
 */
function main() {
    try {
        injectReportMetadata();
        setupPostmanCollectionDownload();
        setupTableResizing();
        initializeListWithFilters();
        const cont = document.querySelector('.page-content');
        cont.classList.add('visible');
    } catch (error) {
        console.error('Error initializing report:', error);
    }
}

/**
 * Initializes the report metadata with values from constants
 */
function injectReportMetadata() {
    // Set document title
    document.title = `${constants.apiName} | ${constants.testingSessionId} | RestTestGen Coverage Report`;

    // Update metadata elements
    DomUtils.updateTextContent(".report-header__meta-value.api-name", constants.apiName);
    DomUtils.updateTextContent(".report-header__meta-value.session-id", constants.testingSessionId);
    DomUtils.updateTextContent(".report-header__meta-value.timestamp", constants.testingSessionTimestamp);
}



/**
 * Sets up the Postman collection download functionality
 */
function setupPostmanCollectionDownload() {
    const downloadReportButton = document.querySelector("#download-report");
    if (!downloadReportButton) {
        console.error("Download button not found");
        return;
    }

    // Set button content with icon
    downloadReportButton.innerHTML = `${constants.icons.postman} Download Postman Collection`;

    // Add click event listener
    downloadReportButton.addEventListener("click", (e) => {
        e.preventDefault();
        e.stopPropagation();

        // Show an alert to confirm or cancel (showing item count)
        const selectedItems = ListWithFilters.getSelectedItemsIds();
        const itemCount = selectedItems.length;

        const alertMessage = itemCount > 0 ?
            `You are about to download a Postman collection with ${itemCount} selected item(s). Do you want to proceed?` :
            `No items selected. Download the Postman collection with all items?`;

        const userConfirmed = window.confirm(alertMessage);
        if (!userConfirmed) {
            return; // User cancelled the download
        }

        // Generate collection

        // If no items selected, use all test sequences, otherwise filter by selected IDs
        const selectedTestSequences = itemCount > 0 ? 
            constants.testSequences.filter(seq => selectedItems.includes(seq.id)) :
            constants.testSequences;

        //console.log("Selected items: ", selectedItems);
        //console.log("Selected test sequences for Postman collection:", selectedTestSequences);

        const postmanCollection = PostmanUtils.generateCollection(selectedTestSequences);

        const url = postmanCollection.getDownloadURL();
        const filename = `postman-collection-${constants.testingSessionId}.json`;

        DomUtils.downloadFile(url, filename);

        return false;
    });
}

/**
 * Sets up event listeners for table resizing
 */
function setupTableResizing() {
    window.addEventListener('load', resizeReportTable);
    window.addEventListener('resize', resizeReportTable);
}

/**
 * Initializes the ListWithFilters component
 */
function initializeListWithFilters() {
    // Store in window object to make it accessible globally if needed
    window.listWithFilters = new ListWithFilters(constants.testSequences);
}

/**
 * Resizes the report table to fit the viewport
 */
function resizeReportTable() {
    const getTableElements = () => {
        const header = document.querySelector('.report-header');
        const filterBar = document.querySelector('.report-content__filter-bar');
        const tableContainer = document.querySelector('.report-content__table-container');

        if (!tableContainer) {
            console.warn('Table container not found');
            return null;
        }

        const table = tableContainer.querySelector('table');
        const thead = table?.querySelector('thead');
        const tbody = table?.querySelector('tbody');

        if (!table || !thead || !tbody || !header || !filterBar) {
            console.warn('One or more table elements not found');
            return null;
        }

        return { header, filterBar, table, thead, tbody };
    }

    // Get all required elements
    const elements = getTableElements();
    if (!elements) return;

    const { header, filterBar, thead, tbody } = elements;

    // Calculate available height
    const viewportHeight = window.innerHeight;
    const headerHeight = DomUtils.getElementOuterHeight(header);
    const filterBarHeight = DomUtils.getElementOuterHeight(filterBar);
    const theadHeight = DomUtils.getElementOuterHeight(thead);
    const bodyPadding = DomUtils.getBodyPadding();

    // Calculate and set the table body height
    const usedHeight = headerHeight + filterBarHeight + theadHeight + bodyPadding;
    const availableHeight = viewportHeight - usedHeight - 10; // 10px buffer
    tbody.style.height = `${availableHeight}px`;

    // Adjust header for scrollbar
    const scrollbarWidth = tbody.offsetWidth - tbody.clientWidth;
    thead.style.paddingRight = `${scrollbarWidth}px`;
}
