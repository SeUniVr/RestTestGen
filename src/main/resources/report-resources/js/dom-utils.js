class DomUtils {
    /**
     * Creates a toast notification with the given message.
     * The toast will automatically disappear after 2 seconds.
     * @param {string} message - The message to display in the toast.
     */
    static showToast(message) {
        // Create a div element with id "toast"
        const toast = document.createElement("div");
        toast.id = "toast";
        document.body.appendChild(toast);

        // Set the text content of the toast
        toast.textContent = message;

        // After 3 seconds, remove the show class from DIV
        setTimeout(function () {
            document.body.removeChild(toast);
        }, 2000);
    }

    /**
     * Updates the text content of an element if it exists
     * @param {string} selector - CSS selector for the element
     * @param {string} value - Text value to set
     */
    static updateTextContent(selector, value) {
        const element = document.querySelector(selector);
        if (element) {
            element.textContent = value;
        } else {
            console.warn(`Element not found: ${selector}`);
        }
    }

    /**
     * Calculates the total body padding
     * @returns {number} Total body padding in pixels
     */
    static getBodyPadding() {
        const style = getComputedStyle(document.body);
        let padding = parseFloat(style.paddingTop) || 0;
        padding += parseFloat(style.paddingBottom) || 0;
        return padding;
    }

    /**
     * Calculates the total outer height of an element including margins
     * @param {HTMLElement} el - Element to measure
     * @returns {number} Total height in pixels
     */
    static getElementOuterHeight(el) {
        const style = getComputedStyle(el);
        const marginTop = parseFloat(style.marginTop) || 0;
        const marginBottom = parseFloat(style.marginBottom) || 0;
        return el.offsetHeight + marginTop + marginBottom;
    }

    /**
     * Downloads a file from a URL with a specified filename
     * @param {string} url - URL to download
     * @param {string} filename - Name for the downloaded file
     */
    static downloadFile(url, filename) {
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
    }

    /**
     * Copy content to clipboard
     * @param {string} content - Content to copy
     */
    static copyToClipboard(content, callback = null) {
        // Copy the command to the clipboard
        navigator.clipboard.writeText(content).then(() => {
            if (callback && typeof callback === 'function') {
                callback();
            }
        }).catch(err => {
            console.error('Failed to copy: ', err);
        });
    }

    /**
     * Escapes HTML special characters in a string
     * @param {string} unsafe - String to escape
     * @returns {string} Escaped string
     */
    static escapeHTML(unsafe) {
        return unsafe
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }
}
