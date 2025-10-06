import {Value, FunctionTrace, StatementTrace, ExpressionTrace, AssignmentExpressionTrace} from "./Def.js";
import {Event, Step} from "./json/EventTrace.js";

/**
 * Translates the given trace to a tree format.
 * @param {Trace} trace - The trace object to be translated.
 * @param {SourceFormat} sourceFormat - The source format containing syntax nodes.
 * @param {ObjectData} objectData - The object data used for value creation.
 * @returns {TraceElement} The translated tree format.
 */
export function translateToTreeFormat(trace, sourceFormat, objectData) {
    let dataStack = [];
    trace.trace = trace.trace.reverse();
    let currentTrace = trace;

    const syntaxNodeCache = createSyntaxNodeCache(sourceFormat);
    const lineNumberToSyntaxNode = createLineNumberToSyntaxNodeMap(sourceFormat);

    while (currentTrace.trace.length > 0) {
        const elem = currentTrace.trace.pop();
        let newTrace = currentTrace;

        if (elem instanceof Event) {
            handleEvent(elem, dataStack, newTrace, lineNumberToSyntaxNode);
        } else if (elem instanceof Step) {
            handleStep(elem, dataStack, newTrace, syntaxNodeCache, objectData);
        }

        currentTrace = newTrace;
    }

    return dataStack[0][0];
}

/**
 * Creates a cache of syntax nodes from the source format.
 * @param {SourceFormat} sourceFormat - The source format containing syntax nodes.
 * @returns {Map[String, SyntaxNode]} The cache of syntax nodes.
 */
function createSyntaxNodeCache(sourceFormat) {
    const syntaxNodeCache = new Map();
    sourceFormat.syntaxNodes.forEach(node => {
        syntaxNodeCache.set(node.identifier, node);
    });
    return syntaxNodeCache;
}

/**
 * Creates a map of line numbers to syntax nodes from the source format.
 * @param {SourceFormat} sourceFormat - The source format containing syntax nodes.
 * @returns {Map[int, SyntaxNode]} The map of line numbers to syntax nodes.
 */
function createLineNumberToSyntaxNodeMap(sourceFormat) {
    const lineNumberToSyntaxNode = new Map();
    sourceFormat.syntaxNodes.forEach(node => {
        if (node.startLine !== undefined) {
            const text = node.getEntireText();
            const oldText = lineNumberToSyntaxNode.get(text);
            if (!oldText || oldText.length < text.length) {
                lineNumberToSyntaxNode.set(node.startLine, text);
            }
        }
    });
    return lineNumberToSyntaxNode;
}

/**
 * Handles an event element in the trace.
 * @param {Event} elem - The event element to handle.
 * @param {Array} dataStack - The stack of data elements.
 * @param {Trace} newTrace - The current trace object.
 * @param {Map[int, SyntaxNode]} lineNumberToSyntaxNode - The map of line numbers to syntax nodes.
 */
function handleEvent(elem, dataStack, newTrace, lineNumberToSyntaxNode) {
    if (elem.eventType === 'controlFlow' && elem.kind.type === 'FunctionContext') {
        handleFunctionContext(elem, dataStack);
    } else if (elem.eventType === 'statement') {
        handleStatement(elem, dataStack, newTrace, lineNumberToSyntaxNode);
    } else if (elem.eventType === 'controlFlow' && elem.kind.type === 'DefaultContext' && elem.pos === 'end') {
        handleDefaultContextEnd(dataStack, lineNumberToSyntaxNode);
    }
}

/**
 * Handles a function context event.
 * @param {Event} elem - The function context event element.
 * @param {Array} dataStack - The stack of data elements.
 */
function handleFunctionContext(elem, dataStack) {
    if (elem.pos === 'start') {
        if (dataStack.length === 0) {
            let mainFunction = new FunctionTrace();
            mainFunction.name = elem.kind.functionName;
            dataStack.push([mainFunction, true]);
        } else {
            let stackElement = dataStack[dataStack.length - 1];
            stackElement[0].name = elem.kind.functionName;
            stackElement[1] = true;
        }
    } else {
        let ft = dataStack[dataStack.length - 1][0];
        if (ft.isVoid) {
            dataStack.pop();
            pushElement(dataStack, ft);
        }
    }
}

/**
 * Handles a statement event.
 * @param {Event} elem - The statement event element.
 * @param {Array} dataStack - The stack of data elements.
 * @param {Trace} newTrace - The current trace object.
 * @param {Map[int, SyntaxNode]} lineNumberToSyntaxNode - The map of line numbers to syntax nodes.
 */
function handleStatement(elem, dataStack, newTrace, lineNumberToSyntaxNode) {
    if (elem.pos === 'start') {
        let st = new StatementTrace();
        dataStack.push([st, true]);
    } else {
        let nextElem = newTrace.trace[newTrace.trace.length - 1];
        if (nextElem instanceof Event && nextElem.eventType === 'controlFlow' && nextElem.kind.type === 'DefaultContext' && nextElem.pos === 'start') {
            newTrace.trace.pop();
        } else {
            let st = dataStack.pop()[0];
            if (st.content.length !== 0) {
                let syntaxNodeInSource = st.content.find(traceElem => traceElem.lineNumber !== undefined);
                st.lineNumber = syntaxNodeInSource.lineNumber;
                pushElement(dataStack, st);
                st.line = lineNumberToSyntaxNode.get(st.lineNumber);
            }
        }
    }
}

/**
 * Handles the end of a default context event.
 * @param {Array} dataStack - The stack of data elements.
 * @param {Map} lineNumberToSyntaxNode - The map of line numbers to syntax nodes.
 */
function handleDefaultContextEnd(dataStack, lineNumberToSyntaxNode) {
    let st = dataStack.pop()[0];
    if (st.content.length !== 0) {
        let syntaxNodeInSource = st.content.find(traceElem => traceElem.lineNumber !== undefined);
        st.lineNumber = syntaxNodeInSource.lineNumber;
        pushElement(dataStack, st);
        st.line = lineNumberToSyntaxNode.get(st.lineNumber);
    }
}

/**
 * Handles a step element in the trace.
 * @param {Step} elem - The step element to handle.
 * @param {Array} dataStack - The stack of data elements.
 * @param {Trace} newTrace - The current trace object.
 * @param {Map} syntaxNodeCache - The cache of syntax nodes.
 * @param {ObjectData} objectData - The object data used for value creation.
 */
function handleStep(elem, dataStack, newTrace, syntaxNodeCache, objectData) {
    if (elem.kind === 'logCall' || (elem.kind === 'logVoidCall' && elem.nodeKey !== 'absent')) {
        handleLogCall(elem, dataStack, newTrace, syntaxNodeCache, objectData);
    } else if (elem.kind === 'logReturn') {
        handleLogReturn(elem, dataStack, objectData);
    } else if (elem.kind === 'expressionWithoutReturn' || elem.kind === 'expression') {
        handleExpression(elem, dataStack, syntaxNodeCache, objectData);
    }
}

/**
 * Handles a log call step.
 * @param {Step} elem - The log call step element.
 * @param {Array} dataStack - The stack of data elements.
 * @param {Trace} newTrace - The current trace object.
 * @param {Map} syntaxNodeCache - The cache of syntax nodes.
 * @param {ObjectData} objectData - The object data used for value creation.
 */
function handleLogCall(elem, dataStack, newTrace, syntaxNodeCache, objectData) {
    let nextElem = newTrace.trace[newTrace.trace.length - 1];
    let ft = new FunctionTrace();
    ft.args = elem.argsValues.map(arg => Value.newValue(arg, objectData));
    let syntaxNodeInSource = syntaxNodeCache.get(elem.nodeKey);
    ft.lineNumber = (syntaxNodeInSource.startLine === undefined) ? '-' : syntaxNodeInSource.startLine;

    if (elem.kind === 'logVoidCall') {
        ft.isVoid = true;
    }

    const isExternalWithoutReturn = (nextElem instanceof Event && (nextElem.eventType === 'statement' || nextElem.eventType === 'subStatement') && nextElem.pos === 'end');
    const isExternalWithReturn = (nextElem instanceof Step && nextElem.kind === 'logReturn');
    ft.isExternal = isExternalWithoutReturn || isExternalWithReturn;

    if (!ft.isExternal) {
        dataStack.push([ft, false]);
    } else {
        if (isExternalWithReturn) {
            ft.returnVal = Value.newValue(nextElem.result, objectData);
            newTrace.trace.pop();
        }

        ft.name = syntaxNodeInSource.getExpressionText();
        pushElement(dataStack, ft);
    }
}

/**
 * Handles a log return step.
 * @param {Step} elem - The log return step element.
 * @param {Array} dataStack - The stack of data elements.
 * @param {ObjectData} objectData - The object data used for value creation.
 */
function handleLogReturn(elem, dataStack, objectData) {
    let ft = dataStack.pop()[0];
    ft.returnVal = Value.newValue(elem.result, objectData);
    pushElement(dataStack, ft);
}

/**
 * Handles an expression step.
 * @param {Step} elem - The expression step element.
 * @param {Array} dataStack - The stack of data elements.
 * @param {Map[String, SyntaxNode]} syntaxNodeCache - The cache of syntax nodes.
 * @param {ObjectData} objectData - The object data used for value creation.
 */
function handleExpression(elem, dataStack, syntaxNodeCache, objectData) {
    let expTrace = new ExpressionTrace();
    if (elem.assigns.length !== 0) {
        expTrace = new AssignmentExpressionTrace();
    }

    let syntaxNodeInSource = syntaxNodeCache.get(elem.nodeKey);
    expTrace.content = syntaxNodeInSource.getExpressionText();
    expTrace.lineNumber = (syntaxNodeInSource.startLine === undefined) ? "-" : syntaxNodeInSource.startLine;
    expTrace.assigns = elem.assigns.map(assign => [Value.newValue(assign.value, objectData), assign.identifier.name]);
    expTrace.result = elem.result ? Value.newValue(elem.result, objectData) : undefined;
    pushElement(dataStack, expTrace);
}

/**
 * Pushes an element to the content of the top element in the stack.
 * @param {Array} dataStack - The stack of data elements.
 * @param {TraceElement} elem - The element to push.
 */
function pushElement(dataStack, elem) {
    const result = dataStack.slice().reverse().findIndex(stackElem => stackElem[1]);
    dataStack[dataStack.length - 1 - result][0].content.push(elem);
    elem.parent = dataStack[dataStack.length - 1 - result][0];
}

