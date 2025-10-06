import {ObjectValue} from "../Def.js";

/**
 * This class is used to parse and execute queries. The parser is basic and only supports a few commands.
 */
export class Parser {
    constructor(trace) {
        this.trace = trace;
    }

    static performComparison(fieldType, fieldActualValue, compareValue, compareFunction) {
        if (fieldType === 'int') {
            fieldActualValue = Number(fieldActualValue);
            compareValue = Number(compareValue);
        } else {
            fieldActualValue = String(fieldActualValue);
            compareValue = String(compareValue);
        }
        return compareFunction(fieldActualValue, compareValue);
    }

    /**
     * Executes a query
     * @param query {string} The query to execute
     * @returns {string} The result of the query
     */
    executeQuery(query) {

        const queryParts = query.split(' ');
        const command = queryParts[0];
        const args = queryParts.slice(1);
        let result = undefined;

        switch (command) {
            case 'toggle':
                this.trace.treeTrace.triangle.toggleAll();
                return "Toggle command executed";

            case 'testSearch':
                result = this.trace.getObjectsGivenCondition((value) => {
                    return (value instanceof ObjectValue);
                });
                return "";

            case 'sf':
                const lineOption = args.find(arg => arg.startsWith('--line='));
                const hasFirstOrLastOption = args.includes('--first') || args.includes('--last');
                const firstOrLastOption = hasFirstOrLastOption ? args[args.length - 1] : null;
                const queryArgs = args.filter(arg => !arg.startsWith('--line=') && arg !== '--first' && arg !== '--last');

                if (args.length < 3) {
                    return 'Usage: sf &lt;class&gt;.&lt;field&gt; &lt;operator&gt; &lt;value&gt; [--first|--last|--line=&lt;number&gt;]';
                }

                const [className, fieldName] = queryArgs[0].split('.');
                const operator = queryArgs[1];
                const valueToCompare = queryArgs[2];

                if (!className || !fieldName) {
                    return 'Invalid class or field name in query';
                }

                const operatorFunctions = {
                    "=": (a, b) => a === b,
                    "==": (a, b) => a === b,
                    "===": (a, b) => a === b,
                    "!=": (a, b) => a !== b,
                    "!==": (a, b) => a !== b,
                    ">=": (a, b) => a >= b,
                    "<=": (a, b) => a <= b,
                    ">": (a, b) => a > b,
                    "<": (a, b) => a < b
                };

                const allResults = this.trace.getObjectsGivenCondition((value) => {
                    if (value instanceof ObjectValue && value.dataType === className) {
                        const compare = operatorFunctions[operator];
                        if (!compare) {
                            console.log(`Unsupported operator: ${operator}`);
                            return false;
                        }

                        return value.fields.some(([fieldKey, fieldValue]) => {
                            if (fieldKey === fieldName) {
                                return Parser.performComparison(
                                    fieldValue.dataType,
                                    fieldValue.value,
                                    valueToCompare,
                                    compare
                                );
                            }
                            return false;
                        });
                    }
                    return false;
                });

                if (lineOption) {
                    const lineNumber = Number(lineOption.split('=')[1]);
                    if (!isNaN(lineNumber)) {
                        result = allResults.filter(obj => obj[0].lineNumber === lineNumber);
                    } else {
                        console.log('Invalid line number in --line=<number> option');
                        return;
                    }
                } else {
                    result = allResults;
                }

                if (firstOrLastOption === '--first') {
                    result = result.slice(0,1) || null;
                } else if (firstOrLastOption === '--last') {
                    result = result.slice(result.length - 1) || null;
                }

                break;

            case 'values':
                if (args.length < 1) {
                    return 'Usage: values &lt;class&gt;.&lt;field&gt;';
                }
                const [cn, fn] = args[0].split('.');
                let arr = new Set();
                this.trace.getObjectsGivenCondition((value) => {
                    if (value instanceof ObjectValue && value.dataType === cn) {
                        value.fields.forEach(([fieldKey, fieldValue]) => {
                            if (fieldKey === fn) {
                                arr.add(fieldValue.value);
                                return true;
                            }
                        });
                    }
                    return false;
                });
                return Array.from(arr).sort().join(', ');
            case 'help':
                return `
<pre>
help:
    display help for queries syntax
    
toggle:
    open/close every segment of the trace

sf &lt;class&gt;.&lt;field&gt; &lt;operator&gt; &lt;value&gt; [--first|--last|--line=&lt;number&gt;]:
    search for every object with class &lt;class&gt; and field value satisfying comparison "&lt;field&gt; &lt;operator&gt; &lt;value&gt;"
        &lt;class&gt;: class name of the object
        &lt;field&gt;: field name for which comparison will be performed
        &lt;operator&gt;: comparison operator. Must be one of [=,==,===,!=,!==,&lt;,&lt;=,&gt;,&gt;=]
        &lt;value&gt;: value to compare with

    Example usage: sf Person.name == Patrick --line=32
    
values &lt;class&gt;.&lt;field&gt;
    list every different value that a field &lt;field&gt; of class &lt;class&gt; takes during program execution
    
    Example usage: values Person.name
</pre>`;
            default:
                return 'Unknown command. Type "help" for a list of commands.';
        }

        return result;
    }
}
