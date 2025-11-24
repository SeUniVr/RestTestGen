class ListWithFilters {
    static #instance; // Singleton instance

    #inputData = null;
    #listObj = null;
    #filters = [];

    static #columnNames = {
        "id": "id",
        "endpoint": "requestURL",
        "method": "requestMethod",
        "code": "responseStatusCode",
        "parameters": "requestParameters",
        "testType": "testType"
    }

    static #valueNames =  ['id', 'endpoint', 'method', 'code', 'parameters', 'testType'];

    static #listContainerClass = ".report-content__table";

    static #filterIdClass = 'filter-id';
    static #filterEndpointClass = 'filter-endpoint';
    static #filterMethodClass = 'filter-method';
    static #filterStatusCodeClass = 'filter-status-code';
    static #filterParametersClass = 'filter-parameters';
    static #filterTestTypeClass = 'filter-test-type';
    static #filterResetButtonClass = 'filter-reset';

    static #noParametersLabelClass = 'no-parameters-label';

    static tableDataIdClass = 'table-data-id';
    static #tableDataEndpointClass = 'table-data-endpoint';
    static #tableDataMethodClass = 'table-data-method';
    static #tableDataStatusCodeClass = 'table-data-code';
    static #tableDataParametersClass = 'table-data-parameters';
    static #tableDataTestTypeClass = 'table-data-test-type'
    static #tableDataActionsClass = 'table-data-actions';

    static checkBoxIdSelectAll = 'select-all-checkbox';
    static checkBoxClass = 'item-checkbox';
    static #buttonViewDetailsClass = 'button-view-details';
    static #buttonDownloadPostmanClass = 'button-download-postman';

    constructor(inputData) {
        // Singleton pattern to ensure only one instance of ListWithFilters exists
        if (ListWithFilters.#instance) {
            return ListWithFilters.#instance;
        } else {
            ListWithFilters.#instance = this;
        }

        if (!inputData) {
            throw new Error("Input data required to build the list.");
        }

        this.#inputData = inputData;

        // Initialize the list and the filters sections
        this.#initList();
        this.#initFilters();
        this.#initCheckboxes();
    }

    #initList() {
        // Get the list container from the dom
        const listContainer = document.querySelector(ListWithFilters.#listContainerClass);

        // Set the list options
        let listOptions = {
            valueNames: ListWithFilters.#valueNames,
            item: (row) => {

                const pathParameters = ParameterUtils.extractPathParameters(row.requestURLReference, row.requestURLStripped);
                const queryParameters = ParameterUtils.extractQueryParameters(row.requestURLStripped);

                let displayedPath = ParameterUtils.getPathWithRenderedPathParameters(row.requestURLReference, pathParameters);

                if (queryParameters.length > 0) {
                    displayedPath += `<span class="parameter-separator">?</span><wbr>`
                }

                for (const idx in queryParameters) {
                    displayedPath += ParameterUtils.getParameterHtml(queryParameters[idx]);
                    if (idx < queryParameters.length - 1) {
                        displayedPath += `<span class="parameter-separator">&amp;</span><wbr>`;
                    }
                }

                if (row.requestBody != null && row.requestBody.length > 1) {
                    displayedPath += `<span class="has-body">+ REQUEST BODY</span>`;
                }

                // Return the HTML for the row
                return `
                    <tr itemid="${row.id}">
                        <td class="table-col col-checkbox">
                            <input type="checkbox" class="${ListWithFilters.checkBoxClass}" data-itemid="${row.id} name="item-checkbox-${row.id}" title="Select/Deselect Item">
                        </td>
                        <td class="table-col col-id ${ListWithFilters.tableDataIdClass}">${row.id}</td>
                        <td class="table-col col-method ${ListWithFilters.#tableDataMethodClass}">
                            <span class="styled-method styled-method-${row.requestMethod.toLowerCase()}">${row.requestMethod}</span>
                        </td>
                        <td class="table-col col-endpoint ${ListWithFilters.#tableDataEndpointClass}">${displayedPath}</td>
                        <td class="table-col col-status-code ${ListWithFilters.#tableDataStatusCodeClass}">
                            <span class="styled-code styled-code-${row.responseStatusCode[0]}">${row.responseStatusCode}</span>
                        </td>
                        <td class="table-col col-test-type ${ListWithFilters.#tableDataTestTypeClass}">
                            <span class="test-type test-type-${row.testType === '+' ? 'positive' : 'negative'}">${row.testType}</span>
                        </td>
                        <td class="table-col col-actions ${ListWithFilters.#tableDataActionsClass}">
                            <a class="${ListWithFilters.#buttonViewDetailsClass}" title="View request details">${constants.icons.details}</a>
                            <a class="${ListWithFilters.#buttonDownloadPostmanClass}" title="Copy request as CURL command">${constants.icons.copy}</a>
                        </td>
                    </tr>
                `;
            },
        }

        // Create the list object
        this.#listObj = new List(listContainer, listOptions, this.#inputData);

        // Add the callbacks to the action buttons
        document.querySelectorAll(ListWithFilters.#listContainerClass + ' tr').forEach((row) => {
            const id = row.getAttribute('itemid');
            const viewDetailsButton = row.querySelector(`.${ListWithFilters.#buttonViewDetailsClass}`);
            const downloadButton = row.querySelector(`.${ListWithFilters.#buttonDownloadPostmanClass}`);

            if (viewDetailsButton) {
                viewDetailsButton.addEventListener('click', () => {
                    this.#viewDetails(id);
                });
            }

            if (downloadButton) {
                downloadButton.addEventListener('click', () => {
                    this.#copyCURLToClipboard(id);
                });
            }
        });
    }

    #initFilters() {
        // Create the text fields
        this.#createTextFieldFilter(ListWithFilters.#columnNames.id, document.querySelector(`.${ListWithFilters.#filterIdClass}`));
        this.#createTextFieldFilter(ListWithFilters.#columnNames.endpoint, document.querySelector(`.${ListWithFilters.#filterEndpointClass}`));
        this.#createTextFieldFilter(ListWithFilters.#columnNames.method, document.querySelector(`.${ListWithFilters.#filterMethodClass}`));
        this.#createTextFieldFilter(ListWithFilters.#columnNames.code, document.querySelector(`.${ListWithFilters.#filterStatusCodeClass}`));
        this.#createTextFieldFilter(ListWithFilters.#columnNames.testType, document.querySelector(`.${ListWithFilters.#filterTestTypeClass}`));
        //this.#createTextFieldFilter(ListWithFilters.#columnNames.parameters, document.querySelector(`.${ListWithFilters.#filterParametersClass}`));

        // Add callback to the reset button
        const resetButton = document.querySelector(`.${ListWithFilters.#filterResetButtonClass}`);

        if (!resetButton) {
            throw new Error("Reset button not found.");
        }

        resetButton.addEventListener('click', () => {
            this.#resetAllFilters();
        });
    }

    #initCheckboxes() {
        const selectAllCheckbox = document.getElementById(ListWithFilters.checkBoxIdSelectAll);
        const itemCheckboxes = document.querySelectorAll(`.${ListWithFilters.checkBoxClass}`);

        if (!selectAllCheckbox) {
            throw new Error("Select all checkbox not found.");
        }

        if (!itemCheckboxes) {
            throw new Error("Item checkboxes not found.");
        }

        // Add event listener to the select all checkbox
        selectAllCheckbox.addEventListener('change', () => {
            const isChecked = selectAllCheckbox.checked;
            itemCheckboxes.forEach((checkbox) => {
                checkbox.checked = isChecked;
            });
        });
    }

    static getSelectedItemsIds() {
        const checkedIds = [];
        const itemCheckboxes = document.querySelectorAll(`.${ListWithFilters.checkBoxClass}`);

        if (!itemCheckboxes) {
            throw new Error("Item checkboxes not found.");
        }

        itemCheckboxes.forEach((checkbox) => {
            if (checkbox.checked) {
                const itemId = parseInt(checkbox.getAttribute('data-itemid'));
                checkedIds.push(itemId);
            }
        });

        return checkedIds;
    }

    static getAllItemsIds() {
        const allIds = [];
        const itemCheckboxes = document.querySelectorAll(`.${ListWithFilters.checkBoxClass}`);

        if (!itemCheckboxes) {
            throw new Error("Item checkboxes not found.");
        }

        itemCheckboxes.forEach((checkbox) => {
            const itemId = parseInt(checkbox.getAttribute('data-itemid'));
            allIds.push(itemId);
        });

        return allIds;
    }

    #createTextFieldFilter(columnName, domInputElement) {
        if (!columnName) {
            throw new Error("Column name is required to create a filter.");
        }

        if (!domInputElement) {
            throw new Error("DOM input element is required to create a filter.");
        }

        const filterDelayTimeout = 150;
        domInputElement.setAttribute('type', 'text');

        let timeout = null;
        domInputElement.addEventListener('input', () => {
            clearTimeout(timeout);
            timeout = setTimeout(() => {
                this.#applyFilters();
            }, filterDelayTimeout);
        });

        this.#filters.push({
            columnName: columnName,
            domElement: domInputElement
        });
    }

    #applyFilters() {
        if (!this.#listObj) {
            throw new Error("List object is not initialized.");
        }

        if (!this.#filters || this.#filters.length === 0) {
            throw new Error("Filters are not initialized.");
        }

        this.#listObj.filter(item => {
            let isValid = true;

            this.#filters.forEach(filter => {
                const filterValue = filter.domElement.value.toLowerCase();

                if (filterValue === '') {
                    return; // Skip empty filters
                }

                // Get the item value for this column
                const itemValue = item.values()[filter.columnName];

                // If we are filtering the parameters, we need to filter for parameter names only
                if (filter.columnName === ListWithFilters.#columnNames.parameters) {
                    const parameters = itemValue.map(param => `${param.name}`).join(' ');
                    if (!parameters.toLowerCase().includes(filterValue)) {
                        isValid = false; // Mark as invalid if this filter doesn't match
                    }
                } else {
                    if (!itemValue) {
                        isValid = false;
                        return;
                    } else if (!itemValue.toString().toLowerCase().includes(filterValue)) {
                        isValid = false; // Mark as invalid if this filter doesn't match
                    }
                }
            });

            return isValid;
        });
    }

    #resetAllFilters() {
        if (!this.#filters || this.#filters.length === 0) {
            throw new Error("Filters are not initialized.");
        }

        this.#filters.forEach(filter => {
            filter.domElement.value = '';
        });

        this.#listObj.filter();
    }

    getRequestData(id) {
        if (!id) {
            throw new Error("ID is required to get request data.");
        }

        const item = this.#listObj.get("id", id)[0];
        if (!item) {
            throw new Error(`Item with ID ${id} not found.`);
        }

        const data = item.values();

        if (!data) {
            throw new Error(`Data for ID ${id} not found.`);
        }

        return data;
    }

    #viewDetails(id) {
        const requestData = this.getRequestData(id);
        new DetailsPopup(requestData);
    }

    #copyCURLToClipboard(id) {
        const requestData = this.getRequestData(id);

        // Create the CURL command
        const curlCommand = PostmanUtils.getCURLCommand(
            PostmanUtils.getFormattedRequest(
                requestData.requestURL,
                requestData.requestMethod,
                requestData.requestHeader,
                requestData.requestBody
            )
        );

        // Copy the command to the clipboard
        DomUtils.copyToClipboard(curlCommand, () => {
            DomUtils.showToast("CURL command copied to clipboard!", "success");
        });
    }
}