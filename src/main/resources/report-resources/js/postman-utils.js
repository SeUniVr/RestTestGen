/**
 * Utility class for working with Postman requests and collections
 */
class PostmanUtils {
    /**
     * Creates a formatted item for a Postman collection
     * @param {string} id - Item ID
     * @param {string} name - Item name
     * @param {Object} request - Request object
     * @returns {Object} Formatted Postman item
     * @throws {Error} If the request format is invalid
     */
    static getFormattedItem(id, name, request) {
        if (!PostmanUtils.isValidRequest(request)) {            
            throw new Error("Invalid request format.");
        }

        return {
            id: id || "",
            name: name || "",
            request: request
        };
    }

    /**
     * Validates if an item has the correct format
     * @param {Object} item - Item to validate
     * @returns {boolean} True if the item is valid, false otherwise
     */
    static isValidItem(item) {
        return typeof item === "object"
            && typeof item.id === "string"
            && typeof item.name === "string"
            && item.request
            && PostmanUtils.isValidRequest(item.request);
    }

    /**
     * Creates a formatted request object
     * @param {string} url - Request URL
     * @param {string} method - HTTP method
     * @param {string} header - Request headers as string
     * @param {string|Object} body - Request body as JSON string or object
     * @returns {Object} Formatted request
     * @throws {Error} If required parameters are missing or invalid
     */
    static getFormattedRequest(url, method, header, body) {
        if (!url || !method) {
            throw new Error("URL and method are required in a request.");
        }

        // Check on method validity
        if (constants.httpMethods.includes(method.toUpperCase())) {
            method = method.toUpperCase();
        } else {
            throw new Error(`Invalid HTTP method: ${method}. Valid methods are: ${constants.httpMethods.join(", ")}`);
        }

        // Parse header safely
        let parsedHeader = [];
        if (header) {
            // Extract "key: value" pairs from header string separated by new lines
            const headerLines = header.split("\n");
            headerLines.forEach(line => {
                const [key, value] = line.split(":").map(part => part.trim());
                if (key && value) {
                    parsedHeader.push({ key: key, value: value });
                }
            });
            header = parsedHeader;
        }

        // Parse body safely
        let parsedBody = {
            mode: "raw",
            raw: ""
        };
        
        if (body) {
            parsedBody.raw = body;
        }

        return {
            url: url,
            method: method,
            header: parsedHeader,
            body: parsedBody
        };
    }

    /**
     * Validates if a request has the correct format
     * @param {Object} request - Request to validate
     * @returns {boolean} True if the request is valid, false otherwise
     */
    static isValidRequest(request) {
        return typeof request === "object"
            && request.url
            && typeof request.url === "string"
            && request.method
            && typeof request.method === "string"
            && constants.httpMethods.includes(request.method)
            && (typeof request.header === "object" || typeof request.header === "string")
            && typeof request.body === "object";
    }

    /**
     * Generates a cURL command for a given request
     * @param {Object} request - Request object
     * @returns {string} The cURL command for the request
     * @throws {Error} If the request format is invalid
     */
    static getCURLCommand(request) {
        console.log("Generating cURL command for request:", request);
        if (!PostmanUtils.isValidRequest(request)) {
            throw new Error("Invalid request format.");
        }

        // Start with the basic curl command
        let curlCommand = `curl -X ${request.method} "${request.url}"`;

        // Add headers if present
        if (request.header && request.header.length > 0) {
            request.header.forEach(header => {
                curlCommand += ` -H '${header.key}: ${header.value}'`;
            });
        }

        // Add body if present
        if (request.body && request.body.raw) {
            curlCommand += ` --data '${request.body.raw}'`;

            // Opinionated: if the raw data is JSON and no Content-Type header is set, add it so Postman can import it correctly
            let isBodyJSON = false;
            try {
                JSON.parse(request.body.raw);
                isBodyJSON = true;
            } catch (e) {
                isBodyJSON = false;
            }

            const hasContentTypeHeader = request.header && request.header.some(header => header.key.toLowerCase() === "content-type");
            if (isBodyJSON && !hasContentTypeHeader) {
                curlCommand += ` -H 'Content-Type: application/json'`;
            }
        }

        return curlCommand;
    }

    /**
     * Generates a Postman collection from raw data
     * @param {Array<Object>} testSequences - Array of interaction objects
     * @returns {PostmanCollection} Generated Postman collection
     * @throws {Error} If the data is not a valid array of objects
     */
    static generateCollection(testSequences) {
        // Check if data is an array of objects
        if (!Array.isArray(testSequences) || testSequences.length === 0 || !testSequences.every(item => typeof item === 'object')) {
            throw new Error("Data must be a non-empty array of objects.");
        }

        // Create a new PostmanCollection instance
        const newCollection = new PostmanCollection(
            `Collection for ${constants.testingSessionId}`,
            `Collection of API calls targeting ${constants.apiName} during testing session with ID ${constants.testingSessionId} and timestamp ${constants.testingSessionTimestamp}`
        )

        // Loop through each interaction in the data array
        for (const testSequence of testSequences) {
            // Check if the testSequence has all the required fields
            if (testSequence.id == null || testSequence.interactionIndex == null || !testSequence.belongsToInteraction ||
                !testSequence.requestURL || !testSequence.requestMethod) {
                throw new Error("Each testSequence must have id, interactionIndex, belongsToInteraction, requestURL, and requestMethod.");
            }

            // Create a formatted request
            const formattedRequest = PostmanUtils.getFormattedItem(
                testSequence.id.toString(),
                `(ID ${testSequence.id}) ${testSequence.belongsToInteraction} [index ${testSequence.interactionIndex}]`,
                PostmanUtils.getFormattedRequest(
                    testSequence.requestURL,
                    testSequence.requestMethod,
                    testSequence.requestHeader,
                    testSequence.requestBody
                )
            );

            // Add the formatted request to the collection
            newCollection.addItem(formattedRequest);
        }

        return newCollection;
    }
}

/**
 * Class representing a Postman collection
 */
class PostmanCollection {
    /**
     * Private collection data structure
     * @private
     */
    #collection = {
        info: {
            name: "Postman Collection Without Name",
            version: "v2.1.0",
            description: "",
            schema: "https://schema.getpostman.com/json/collection/v2.1.0/"
        },
        item: []
    }

    /**
     * Creates a new PostmanCollection instance
     * @param {string} name - Name of the collection
     * @param {string} description - Description of the collection
     */
    constructor(name, description) {
        this.#collection.info.name = name || "Postman Collection Without Name";
        this.#collection.info.description = description || "";
    }

    /**
     * Adds an item to the Postman collection
     * @param {Object} item - Item object to add
     * @throws {Error} If the request format is invalid
     */
    addItem(item) {
        if (!PostmanUtils.isValidItem(item)) {
            throw new Error("Invalid item format. Cannot add to collection.");
        }

        this.#collection.item.push(item);
    }

    /**
     * Gets the URL to download the collection
     * @returns {string} URL to download the collection
     */
    getDownloadURL() {
        const collectionJSON = JSON.stringify(this.#collection, null, 2);
        const blob = new Blob([collectionJSON], { type: "application/json" });
        return URL.createObjectURL(blob);
    }
}
