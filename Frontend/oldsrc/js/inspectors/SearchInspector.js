import {PWElement} from "../elements/PWElement.js";

/**
 * This class is used to manage the object inspector
 */
export class SearchInspector extends PWElement {

    static instance = undefined;
    static NO_RES_FOUND = document.createTextNode("No result matching query");

    constructor() {
        const element = document.createElement('div');
        element.classList.add('inspectorContent');
        element.id = 'searchInspector';
        super(element);

        if (SearchInspector.instance === undefined) {
            SearchInspector.instance = this;
        } else {
            throw new Error("Only one instance of ObjectInspector is allowed");
        }

        this.initDesign();
    }

    /**
     * Initializes the design of the search inspector
     */
    initDesign() {
        const div = document.createElement('div');
        div.classList.add('searchInspector_header');

        const input = document.createElement('input');
        input.id = 'queryBar';
        input.type = 'text';
        input.placeholder = 'Enter query';
        input.addEventListener("keyup", (event) => {
            if (event.key === "Enter") {
                this.update(pw.parser.executeQuery(input.value), input.value);
            }
        });

        const submitButton = document.createElement('button');
        submitButton.innerHTML = '<i class="bi bi-search"></i>';
        submitButton.addEventListener("click", () => {
            this.update(pw.parser.executeQuery(input.value), input.value);
        });

        const resultDiv = document.createElement('div');
        resultDiv.classList.add('searchInspector_results');

        const resultHeaderDiv = document.createElement('div');
        resultHeaderDiv.classList.add('searchInspector_results_header');
        resultHeaderDiv.appendChild(document.createTextNode('Result:'));

        div.appendChild(input);
        div.appendChild(submitButton);
        this.element.appendChild(div);
        this.element.appendChild(resultHeaderDiv);
        this.element.appendChild(resultDiv);
        this.resultDiv = resultDiv;
        this.resultHeaderDiv = resultHeaderDiv;
    }

    /**
     * Updates the search inspector with the given query result
     * @param queryResult {string|Array} The result of the query
     * @param query {string} The query that was executed
     */
    update(queryResult, query) {
        this.clearResult();

        this.resultHeaderDiv.innerHTML = 'Result for query <span class="searchInspector_query">' + query + '</span>:';

        if (typeof queryResult === 'string') {
            this.resultDiv.innerHTML = queryResult;
        }
        else if (queryResult && queryResult.length > 0) {

            queryResult.forEach(res => {
                const fragment = res[1].documentFragment();

                const viewInTrace = document.createElement('button');
                viewInTrace.classList.add('viewInTrace');
                viewInTrace.innerHTML = '<i class="bi bi-search"></i>';

                viewInTrace.addEventListener('click', () => {
                    const traceElement = res[0];
                    let tempTraceElement = traceElement.parent;

                    while (tempTraceElement !== undefined) {
                        tempTraceElement.element.style.backgroundColor = '';
                        const triangle = tempTraceElement.triangle;
                        if (triangle) {
                            triangle.expand();
                        }
                        console.log(tempTraceElement.parent);
                        tempTraceElement = tempTraceElement.parent;
                    }

                    traceElement.element.scrollIntoView({behavior: 'smooth', block: 'center'});
                });

                viewInTrace.addEventListener('mouseenter', () => {
                    let traceElement = res[0]
                    traceElement.element.style.backgroundColor = 'rgba(147,74,172,0.5)';

                    while (traceElement !== undefined && traceElement.element.parentNode !== undefined) {
                        if (!traceElement.element.parentNode.classList.contains('hidden')) {
                            traceElement.element.style.backgroundColor = 'rgba(147,74,172,0.5)';
                            break;
                        }
                        traceElement = traceElement.parent;
                    }
                });

                viewInTrace.addEventListener('mouseleave', () => {
                    let traceElement = res[0];

                    while (traceElement !== undefined && traceElement.element.parentNode !== undefined) {
                        traceElement.element.style.backgroundColor = '';
                        traceElement = traceElement.parent;
                    }
                });

                const objectResDiv = document.createElement('div');
                objectResDiv.classList.add('searchInspector_result');
                objectResDiv.appendChild(fragment)
                objectResDiv.appendChild(viewInTrace);
                objectResDiv.appendChild(document.createElement('br'));
                this.resultDiv.appendChild(objectResDiv);
            });
        } else {
            this.resultDiv.appendChild(SearchInspector.NO_RES_FOUND);
        }


    }

    /**
     * Clears the search inspector
     */
    clearResult() {
        this.resultDiv.innerHTML = '';
        this.resultHeaderDiv.innerHTML = '';
        this.resultHeaderDiv.appendChild(document.createTextNode('Result:'));
    }
}
