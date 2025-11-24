class DetailsPopup {
    static #instance = null;

    // DOM elements
    #overlay = null;
    #popupElement = null;

    // CSS classes
    static #overlayClass = 'popup-overlay';
    static #popUpClass = 'details-popup';
    static #itemDocumentedTrueClass = 'item-documented-true';
    static #itemTestedTrueClass = 'item-tested-true';
    static #itemDocumentedFalseClass = 'item-documented-false';
    static #itemTestedFalseClass = 'item-tested-false';

    // Handlers
    #escKeyHandler = null;
    #clickOutsideHandler = null;
    #focusTrapper = null;

    // Data
    #interactionData = null;

    constructor(interactionData) {
        // If an instance already exists, close the popup and
        // replace the instance with the new one
        if (DetailsPopup.#instance) {
            console.warn("A DetailsPopup instance already exists. Replacing it with the new one.");
            DetailsPopup.#instance.#close();
        }

        DetailsPopup.#instance = this;

        this.#interactionData = interactionData;
        this.#createPopupAndOverlay();
        this.#setupEventListeners();
        this.#show();
    }

    #createPopupAndOverlay() {
        // Create overlay
        this.#overlay = document.createElement('div');
        this.#overlay.classList.add(DetailsPopup.#overlayClass);

        // Create popup
        this.#popupElement = document.createElement('div');
        this.#popupElement.classList.add(DetailsPopup.#popUpClass);
        this.#createPopupHeader();
        this.#createPopupContent();
    }

    #createPopupHeader() {
        const headerHTML = `
            <header class="popup-header">
                <h2>Interaction details</h2>
                <button class="close-button" aria-label="Close popup">
                    ${constants.icons.x}
                </button>
            </header>
        `;

        const header = document.createElement('template');
        header.innerHTML = headerHTML;

        // Close button handler
        const closeButton = header.content.querySelector('.close-button');
        closeButton.addEventListener('click', () => this.#close());

        this.#popupElement.appendChild(header.content);
    }

    #createPopupContent() {
        const contentHTML = `
            <div class="popup-content">
                <!-- Actions -->
                <div class="popup-content__actions">
                    <button class="popup-content__action-button action-view" aria-label="View raw JSON">View raw JSON</button>
                    <button class="popup-content__action-button action-copy" aria-label="Copy as CURL command">Copy as CURL command</button>
                </div>
                
                <!-- Labels -->
                <div class="popup-content__label">
                    <span class="popup-content__label--key">Genearator</span>
                    <span class="popup-content__label--value">${this.#interactionData.generator}</span>
                </div>

                <div class="popup-content__label">
                    <span class="popup-content__label--key">Global index</span>
                    <span class="popup-content__label--value">${this.#interactionData.id}</span>
                </div>

                <div class="popup-content__label">
                    <span class="popup-content__label--key">Belongs to test sequence</span>
                    <span class="popup-content__label--value">${this.#interactionData.belongsToInteraction} (index ${this.#interactionData.interactionIndex})</span>
                </div>
                
                <!-- Coverage -->
                <div class="coverage-item">
                    <span class="coverage-item__title">Path coverage</span>
                    <span class="coverage-item__value item-path">${this.#interactionData.requestURLReference}</span>
                    <span 
                        class="coverage-item__documented item-path ${this.#interactionData.requestURLIsDocumented ? DetailsPopup.#itemDocumentedTrueClass : DetailsPopup.#itemDocumentedFalseClass}"
                        title="${this.#interactionData.requestURLIsDocumented ? 'Documented' : 'Not documented'}"
                    >
                        ${this.#interactionData.requestURLIsDocumented ? constants.icons.documented : constants.icons.notDocumented}
                    </span>

                    <span 
                        class="coverage-item__tested item-path ${this.#interactionData.requestURLIsTested ? DetailsPopup.#itemTestedTrueClass : DetailsPopup.#itemTestedFalseClass}"
                        title="${this.#interactionData.requestURLIsTested ? 'Tested' : 'Not tested'}">${this.#interactionData.requestURLIsTested ? constants.icons.tested : constants.icons.notTested}
                    </span>
                </div>
                
                <div class="coverage-item">
                    <span class="coverage-item__title">Operation coverage</span>
                    <span class="coverage-item__value item-path">${this.#interactionData.requestMethod}</span>
                    <span
                        class="coverage-item__documented item-path ${this.#interactionData.requestMethodIsDocumented ? DetailsPopup.#itemDocumentedTrueClass : DetailsPopup.#itemDocumentedFalseClass}"
                        title="${this.#interactionData.requestMethodIsDocumented ? 'Documented' : 'Not documented'}"
                    >    
                        ${this.#interactionData.requestMethodIsDocumented ? constants.icons.documented : constants.icons.notDocumented}
                    </span>

                    <span
                        class="coverage-item__tested item-path ${this.#interactionData.requestMethodIsTested ? DetailsPopup.#itemTestedTrueClass : DetailsPopup.#itemTestedFalseClass}"
                        title="${this.#interactionData.requestMethodIsTested ? 'Tested' : 'Not tested'}"
                    >
                        ${this.#interactionData.requestMethodIsTested ? constants.icons.tested : constants.icons.notTested}
                    </span>
                </div>
                
                <div class="coverage-item">
                    <span class="coverage-item__title">Status code coverage</span>
                    <span class="coverage-item__value item-path">${this.#interactionData.responseStatusCode}</span>
                    <span
                        class="coverage-item__documented item-path ${this.#interactionData.responseStatusCodeIsDocumented ? DetailsPopup.#itemDocumentedTrueClass : DetailsPopup.#itemDocumentedFalseClass}"
                        title="${this.#interactionData.responseStatusCodeIsDocumented ? 'Documented' : 'Not documented'}"
                    >    
                        ${this.#interactionData.responseStatusCodeIsDocumented ? constants.icons.documented : constants.icons.notDocumented}
                    </span>
                    <span
                        class="coverage-item__tested item-path ${this.#interactionData.responseStatusCodeIsTested ? DetailsPopup.#itemTestedTrueClass : DetailsPopup.#itemTestedFalseClass}"
                        title="${this.#interactionData.responseStatusCodeIsTested ? 'Tested' : 'Not tested'}"
                    >
                        ${this.#interactionData.responseStatusCodeIsTested ? constants.icons.tested : constants.icons.notTested}
                    </span>
                </div>
                
                <div class="path-parameters-list-container">
                    <span class="parameters-list__title">Path parameters</span>
                    <ul class="item-path-parameters-list">
                    </ul>
                </div>
                
                <div class="query-parameters-list-container">
                    <span class="parameters-list__title">Query parameters</span>
                    <ul class="item-query-parameters-list">
                    </ul>
                </div>

                <!-- Request Headers and Body -->

                <div class="request-headers-container">
                    <span class="request-headers__title">Request headers</span>
                    <pre class="request-headers__content">${DomUtils.escapeHTML(this.#interactionData.requestHeader || 'No request headers')}</pre>
                </div>

                <div class="request-body-container">
                    <span class="request-body__title">Request body</span>
                    <pre class="request-body__content">${this.#interactionData.requestBody ? DomUtils.escapeHTML(this.#interactionData.requestBody) : 'No request body'}</pre>
                </div>

                <!-- Response Headers and Body -->

                <div class="response-headers-container">
                    <span class="response-headers__title">Response headers</span>
                    <pre class="response-headers__content">${DomUtils.escapeHTML(this.#interactionData.responseHeader || 'No response headers')}</pre>
                </div>

                <div class="response-body-container">
                    <span class="response-body__title">Response body</span>
                    <pre class="response-body__content">${this.#interactionData.responseBody ? DomUtils.escapeHTML(this.#interactionData.responseBody) : 'No response body'}</pre>
                </div>                
            </div>
        `;

        const content = document.createElement('template');
        content.innerHTML = contentHTML;

        // View raw JSON button handler
        const viewButton = content.content.querySelector('.popup-content__action-button.action-view');
        viewButton.addEventListener('click', () => {
            // Pretty print JSON data in a new browser tab
            const jsonData = JSON.stringify(this.#interactionData, null, 2);
            const jsonBlob = new Blob([jsonData], { type: 'application/json' });
            const jsonUrl = URL.createObjectURL(jsonBlob);
            const newTab = window.open(jsonUrl, '_blank');
            if (newTab) {
                newTab.focus();
            } else {
                console.error("Failed to open new tab. Please allow pop-ups for this site.");
            }
        });

        // Copy as CURL command button handler
        const copyButton = content.content.querySelector('.popup-content__action-button.action-copy');
        copyButton.addEventListener('click', () => {
            const curlCommand = PostmanUtils.getCURLCommand(
                PostmanUtils.getFormattedRequest(
                    this.#interactionData.requestURL,
                    this.#interactionData.requestMethod,
                    this.#interactionData.requestHeader,
                    this.#interactionData.requestBody
                )
            );
            DomUtils.copyToClipboard(curlCommand, () => {
                DomUtils.showToast("CURL command copied to clipboard!");
            });
        });

        // Populate path parameters list
        const pathParametersList = content.content.querySelector('.item-path-parameters-list');
        const pathParameters = ParameterUtils.extractPathParameters(this.#interactionData.requestURLReference,
            this.#interactionData.requestURLStripped);
        if (pathParameters.length === 0) {
            const noParamsItem = document.createElement('li');
            noParamsItem.classList.add('no-parameters');
            noParamsItem.textContent = "No path parameters in this request.";
            pathParametersList.appendChild(noParamsItem);
        } else {
            for (const idx in pathParameters) {
                const item = document.createElement('li');
                item.classList.add('item-parameter');
                item.innerHTML = ParameterUtils.getParameterHtml(pathParameters[idx]);
                pathParametersList.appendChild(item);
            }
        }

        // Populate query parameters list
        const queryParametersList = content.content.querySelector('.item-query-parameters-list');
        const queryParameters = ParameterUtils.extractQueryParameters(this.#interactionData.requestURLStripped);
        if (queryParameters.length === 0) {
            const noParamsItem = document.createElement('li');
            noParamsItem.classList.add('no-parameters');
            noParamsItem.textContent = "No query parameters in this request.";
            queryParametersList.appendChild(noParamsItem);
        } else {
            for (const idx in queryParameters) {
                const item = document.createElement('li');
                item.classList.add('item-parameter');
                item.innerHTML = ParameterUtils.getParameterHtml(queryParameters[idx]);
                queryParametersList.appendChild(item);
            }
        }

        // Populate query parameters list (old implementation)
        /*
        const parametersList = content.content.querySelector('.item-query-parameters-list');
        const paramsData = this.#getParametersData();

        if (!paramsData || Object.keys(paramsData).length === 0) {
            const noParamsItem = document.createElement('li');
            noParamsItem.classList.add('no-parameters');
            noParamsItem.textContent = "No query parameters in this request.";
            parametersList.appendChild(noParamsItem);
        } else {
            const paramsListFragment = document.createDocumentFragment();

            for (const paramName in paramsData) {
                const param = paramsData[paramName];
                const item = document.createElement('li');
                item.classList.add('item-parameter');

                // Create parameter header
                const paramHeader = document.createElement('div');
                paramHeader.classList.add('item-parameter-header');
                paramHeader.innerHTML = `
                    <span class="param-name">${param.name}</span>
                    <span 
                        class="param-is-documented ${param.isDocumented ? DetailsPopup.#itemDocumentedTrueClass : DetailsPopup.#itemDocumentedFalseClass}"
                        title="${param.isDocumented ? 'Documented' : 'Not documented'}"
                    >    
                        ${param.isDocumented ? constants.icons.documented : constants.icons.notDocumented}
                    </span>

                    <span 
                        class="param-is-tested ${param.isTested ? DetailsPopup.#itemTestedTrueClass : DetailsPopup.#itemTestedFalseClass}"
                        title="${param.isTested ? 'Tested' : 'Not tested'}"
                    >
                        ${param.isTested ? constants.icons.tested : constants.icons.notTested}
                    </span>
                `;
                item.appendChild(paramHeader);

                // Create values list
                const valuesList = document.createElement('ul');
                valuesList.classList.add('item-parameter-values');

                if (!param.values || param.values.length === 0) {
                    const noValuesItem = document.createElement('li');
                    noValuesItem.classList.add('no-values');
                    noValuesItem.textContent = "No values for this parameter";
                    valuesList.appendChild(noValuesItem);
                } else {
                    param.values.forEach(paramValue => {
                        const valueItem = document.createElement('li');
                        valueItem.classList.add('item-parameter-value');
                        valueItem.innerHTML = `
                            <span class="param-value">${DomUtils.escapeHTML(paramValue.value)}</span>
                            <span
                                class="param-value-is-documented ${paramValue.isDocumented ? DetailsPopup.#itemDocumentedTrueClass : DetailsPopup.#itemDocumentedFalseClass}"
                                title="${paramValue.isDocumented ? 'Documented' : 'Not documented'}"
                            >
                                ${paramValue.isDocumented ? constants.icons.documented : constants.icons.notDocumented}
                            </span>

                            <span
                                class="param-value-is-tested ${paramValue.isTested ? DetailsPopup.#itemTestedTrueClass : DetailsPopup.#itemTestedFalseClass}"
                                title="${paramValue.isTested ? 'Tested' : 'Not tested'}"
                            >
                                ${paramValue.isTested ? constants.icons.tested : constants.icons.notTested}
                            </span>
                        `;
                        valuesList.appendChild(valueItem);
                    });
                }

                item.appendChild(valuesList);
                paramsListFragment.appendChild(item);
            }

            parametersList.appendChild(paramsListFragment);
        } */

        // Populate request body (pretty print JSON if applicable)
        this.#populateRequestBody(content.content);

        // Populate response body (pretty print JSON if applicable)
        this.#populateResponseBody(content.content);

        // Append everything to the popup content
        this.#popupElement.appendChild(content.content);
    }

    #populateRequestBody(contentElement) {
        const requestBodyContainer = contentElement.querySelector('.request-body-container');
        const requestBodyContent = contentElement.querySelector('.request-body__content');
        const rawRequestBody = this.#interactionData.requestBody;

        if (rawRequestBody) {
            try {
                const parsedBody = JSON.parse(rawRequestBody);
                const prettyBody = JSON.stringify(parsedBody, null, 2);
                requestBodyContent.textContent = prettyBody;
            } catch (e) {
                // Not a JSON body, leave as is
                requestBodyContent.textContent = DomUtils.escapeHTML(rawRequestBody);
            }
        } else {
            requestBodyContent.textContent = 'No request body';
        }
    }

    #populateResponseBody(contentElement) {
        const responseBodyContainer = contentElement.querySelector('.response-body-container');
        const responseBodyContent = contentElement.querySelector('.response-body__content');
        const rawResponseBody = this.#interactionData.responseBody;

        if (rawResponseBody) {
            try {
                const parsedBody = JSON.parse(rawResponseBody);
                const prettyBody = JSON.stringify(parsedBody, null, 2);
                responseBodyContent.textContent = prettyBody;
            } catch (e) {
                // Not a JSON body, leave as is
                responseBodyContent.textContent = DomUtils.escapeHTML(rawResponseBody);
            }
        } else {
            responseBodyContent.textContent = 'No response body';
        }
    }


    #getParametersData() {
        // Get parameters from requestData
        if (this.#interactionData.requestParameters.length === 0) {
            return {};
        }

        // Create an object for each parameter
        // Values referring to the same parameter name will be grouped together

        const paramsData = {};

        this.#interactionData.requestParameters.forEach(parameter => {
            if (!paramsData[parameter.name]) {
                paramsData[parameter.name] = {
                    name: parameter.name,
                    isDocumented: parameter.isDocumented,
                    isTested: parameter.isTested,
                    values: []
                };
            }

            paramsData[parameter.name].values.push(parameter.parameterValue); // Contains already documented and tested values
        });

        return paramsData;
    }

    #setupEventListeners() {
        // Escape key handler
        this.#escKeyHandler = (event) => {
            if (event.key === 'Escape') {
                this.#close()
            }
        };
        document.addEventListener('keydown', this.#escKeyHandler);

        // Click outside handler
        this.#clickOutsideHandler = (event) => {
            if (event.target === this.#overlay) {
                this.#close()
            }
        };
        this.#overlay.addEventListener('click', this.#clickOutsideHandler);

        // Focus trapper
        this.#focusTrapper = (event) => {
            const focusableElements = this.#popupElement.querySelectorAll('button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])');
            const firstElement = focusableElements[0];
            const lastElement = focusableElements[focusableElements.length - 1];

            if (event.key === 'Tab') {
                if (event.shiftKey) { // Shift + Tab
                    if (document.activeElement === firstElement) {
                        event.preventDefault();
                        lastElement.focus();
                    }
                } else { // Tab
                    if (document.activeElement === lastElement) {
                        event.preventDefault();
                        firstElement.focus();
                    }
                }
            }
        }

        this.#popupElement.addEventListener('keydown', this.#focusTrapper);
    }

    #show() {
        document.body.appendChild(this.#overlay);
        document.body.appendChild(this.#popupElement);
    }

    #close() {
        if (this.#overlay) {
            document.body.removeChild(this.#overlay);
        }
        if (this.#popupElement) {
            document.body.removeChild(this.#popupElement);
        }

        // Remove event listeners
        document.removeEventListener('keydown', this.#escKeyHandler);
        this.#overlay.removeEventListener('click', this.#clickOutsideHandler);
        this.#popupElement.removeEventListener('keydown', this.#focusTrapper);

        DetailsPopup.#instance = null;
    }
}
