class ParameterUtils {

    // Extracts a list of query parameters [{name, value}, ...] from a URL
    static extractQueryParameters(url) {

        const question_mark_index = url.indexOf("?");

        if (question_mark_index === -1) {
            return [];
        }

        let parameters = [];
        const query_parameters_string = url.substring(question_mark_index + 1);

        if (query_parameters_string.length > 1) {
            let query_parameter_string = query_parameters_string.split("&");
            for (let i = 0; i < query_parameter_string.length; i++) {
                let query_parameter_string_split = query_parameter_string[i].split("=");
                parameters.push({name: query_parameter_string_split[0], value: query_parameter_string_split[1]})
            }
        }

        return parameters;
    }

    // Extracts a list of path parameters [{name, value}, ...] from a template path and a concrete path
    static extractPathParameters(templatePath, concretePath) {

        // Clean trails and remove query parameters
        templatePath = this.removeTrailingSlash(templatePath);
        concretePath = this.removeTrailingSlash(concretePath);
        concretePath = this.extractPath(concretePath);

        const templateParts = templatePath.split('/');
        const concreteParts = concretePath.split('/');

        if (templateParts.length !== concreteParts.length) {
            return [];
        }

        var pathParameters = [];

        templateParts.forEach((part, idx) => {

            // TODO: improve with regex to support partial path parameters
            if (part.length > 2 && part.startsWith('{') && part.endsWith('}')) {
                const parameterName = part.substring(1, part.length - 1);
                pathParameters.push({name: parameterName, value: concreteParts[idx]});
            }
        });

        return pathParameters;
    }

    static getPathWithRenderedPathParameters(path, pathParameters) {
        for (const idx in pathParameters) {
            path = path.replace(`{${pathParameters[idx].name}}`, `<span class="parameter-separator">{</span>${ParameterUtils.getParameterHtml(pathParameters[idx])}<span class="parameter-separator">}</span>`);
        }
        return path;
    }

    static removeTrailingSlash(str) {
        if (str.endsWith('/')) {
            return str.slice(0, -1);
        }
        return str;
    }

    static extractPath(url) {
        const question_mark_index = url.indexOf("?");
        if (question_mark_index > -1) {
            url = url.substring(0, question_mark_index)
        }
        return url;
    }

    static getParameterHtml(parameter) {
        return `<span class="parameter-name">${parameter.name}</span><span class="parameter-equals">=</span><span class="parameter-value">${parameter.value}</span>`;
    }
}