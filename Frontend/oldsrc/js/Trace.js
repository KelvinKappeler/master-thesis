import {TraceTriangle} from "./elements/TraceTriangle.js";
import {ObjectValue} from "./Def.js";

/**
 * This class is used to show the trace of the program execution.
 */
export class Trace {
    static lineNumbersArea = document.querySelector('.lineNumbers');
    static trianglesArea = document.querySelector('.traceTriangles');
    static traceContentArea = document.querySelector('.traceContent');
    static space = "  ";

    constructor(treeTrace) {
        this.treeTrace = treeTrace;

        this.blockStack = [new TraceBlock()];
    }

    /**
     * Get the last block in the stack.
     * @returns {TraceBlock} The last block in the stack.
     */
    getLastBlock() {
        return this.blockStack[this.blockStack.length - 1];
    }

    /**
     * Display the trace by appending the tree trace to the DOM.
     */
    show() {
        Trace.lineNumbersArea.innerHTML = '';
        Trace.trianglesArea.innerHTML = '';
        Trace.traceContentArea.innerHTML = '';

        this.treeTrace.append(this);
        const lineNumbers = this.blockStack[0].stackFragment.linesNumber;

        Trace.lineNumbersArea.appendChild(lineNumbers);
        Trace.trianglesArea.appendChild(this.blockStack[0].stackFragment.triangles);
        Trace.traceContentArea.appendChild(this.blockStack[0].stackFragment.traceContent);
    }

    /**
     * Create a new block in the trace.
     * @param {number} lineNumber - The line number to start the block.
     * @param {Node} headerFragment - The header content of the block.
     * @param {boolean} isHidden - Whether the block should be initially hidden.
     */
    createBlock(lineNumber, headerFragment, isHidden) {
        const newBlock = new TraceBlock();

        const blockTriangle = this.addLine(lineNumber, headerFragment, newBlock, isHidden);

        newBlock.stackFragment.linesNumber.classList.add('lineNumberBlock');
        newBlock.stackFragment.triangles.classList.add('trianglesBlock');
        newBlock.stackFragment.traceContent.classList.add('codeBlocks');

        if (isHidden) {
            newBlock.stackFragment.linesNumber.classList.add('hidden');
            newBlock.stackFragment.triangles.classList.add('hidden');
            newBlock.stackFragment.traceContent.classList.add('hidden');
        }

        this.blockStack.push(newBlock);

        return blockTriangle;
    }

    /**
     * Close the current block and append it to the previous block.
     */
    closeBlock() {
        const bs = this.blockStack.pop();
        const lastBlock = this.getLastBlock();
        lastBlock.subTriangles = lastBlock.subTriangles.concat(bs.subTriangles);
        lastBlock.stackFragment.linesNumber.appendChild(bs.stackFragment.linesNumber);
        lastBlock.stackFragment.triangles.appendChild(bs.stackFragment.triangles);
        lastBlock.stackFragment.traceContent.appendChild(bs.stackFragment.traceContent);
    }

    /**
     * Add a line to the current block.
     * @param {number} lineNumber - The line number to add.
     * @param {Node} contentFragment - The content of the line.
     * @param {TraceBlock} [newBlock=null] - The new block to create.
     * @param {boolean} [isNewBlockHidden=false] - Whether the new block should be initially hidden.
     */
    addLine(lineNumber, contentFragment, newBlock = null, isNewBlockHidden = false) {
        const lastBlock = this.getLastBlock();
        lastBlock.stackFragment.linesNumber.appendChild(document.createTextNode(lineNumber.toString()));
        lastBlock.stackFragment.linesNumber.appendChild(document.createElement('br'));

        const newLines = this.countNewLinesInDocumentFragment(contentFragment);
        contentFragment.prepend(document.createTextNode(Trace.space.repeat(this.blockStack.length - 1)));
        lastBlock.stackFragment.traceContent.appendChild(contentFragment);
        lastBlock.stackFragment.traceContent.appendChild(document.createElement('br'));

        let triangle = null;
        if (newBlock) {
            triangle = new TraceTriangle(newBlock, isNewBlockHidden);
            triangle.attachTo(lastBlock.stackFragment.triangles);
            lastBlock.subTriangles.push(triangle);
        }
        lastBlock.stackFragment.triangles.appendChild(document.createElement('br'));

        for (let i = 0; i < newLines; i++) {
            lastBlock.stackFragment.linesNumber.appendChild(document.createTextNode((lineNumber + i + 1).toString()));
            lastBlock.stackFragment.linesNumber.appendChild(document.createElement('br'));
            lastBlock.stackFragment.triangles.appendChild(document.createElement('br'));
        }

        return triangle;
    }

    /**
     * Get the first element of the trace that contains the specified object version.
     * @param {ObjectValue} objectValue - The object value to search for.
     * @returns {TraceElement} The trace element that contains object.
     */
    getTraceElementWithObjectValue(objectValue) {
        let results = this.getObjectsGivenCondition((value) =>
            value instanceof ObjectValue && value.isLowerVersion(objectValue)
        )
        results = results.reduce((prev, curr) => prev[1].version < curr[1].version ? prev : curr);
        return results[0];
    }

    /**
     * Count the number of new lines in a document fragment.
     * @param {Node} fragment - The document fragment to count new lines in.
     * @returns {number} The number of new lines in the document fragment.
     */
    countNewLinesInDocumentFragment(fragment) {
        let count = 0;
        fragment.childNodes.forEach(node => {
            if (node.nodeType === Node.TEXT_NODE) {
                count += (node.textContent.match(/\n/g) || []).length;
            } else if (node.nodeType === Node.ELEMENT_NODE) {
                count += this.countNewLinesInDocumentFragment(node);
            }
        });
        return count;
    }

    /**
     * search for all object values in the trace that satisfy the given condition.
     * @param {function(ObjectValue): boolean} condition - The condition to search for.
     * @returns {FlatArray<*, 1>[]}
     */
    getObjectsGivenCondition(condition) {
        return this.treeTrace.searchObject(condition).flat();
    }
}

export class TraceBlock {
    constructor() {
         this.stackFragment = new StackFragment(
             document.createElement('div'),
             document.createElement('div'),
             document.createElement('div'));
         this.subTriangles = [];
    }
}

/**
 * Class representing a fragment of the stack.
 */
class StackFragment {
    constructor(linesNumber, triangles, traceContent) {
        this.linesNumber = linesNumber;
        this.triangles = triangles
        this.traceContent = traceContent;
    }
}

/**
 * Enum for trace span types.
 */
export class TraceSpanType {
    static Parenthesis = new TraceSpanType("parenthesis");
    static ReturnValue = new TraceSpanType("returnValue");
    static ReturnValuePrimitive = new TraceSpanType("returnValuePrimitive");
    static LoopHeaderAssignment = new TraceSpanType("loopHeaderAssignment");
    static LoopHeaderCondition = new TraceSpanType("loopHeaderCondition");
    static FunctionName = new TraceSpanType("functionName");
    static ArgsValue = new TraceSpanType("argsValue");
    static ArgsValuePrimitive = new TraceSpanType("argsValuePrimitive");
    static Keywords = new TraceSpanType("keywords");
    static None = new TraceSpanType("none");

    constructor(name) {
        this.name = name;
    }
}

/**
 * Class for creating and managing trace spans.
 */
export class TraceSpan {
    static keywords = [
        "abstract", "continue", "for", "new", "switch",
        "default", "do", "if", "private", "this",
        "break", "double", "implements", "protected", "throw",
        "byte", "else", "import", "public", "throws",
        "case", "enum", "instanceof", "return", "transient",
        "catch", "extends", "int", "short", "try",
        "char", "final", "interface", "static", "void",
        "class", "finally", "long", "volatile", "float",
        "native", "super", "while"
    ];

    /**
     * Create a span element with the specified category and text content.
     * @param {TraceSpanType} category - The category of the span.
     * @param {string} textContent - The text content of the span.
     * @returns {HTMLSpanElement} The created span element.
     */
    static createSpan(category, textContent) {
        const span = document.createElement('span');
        span.classList.add(category.name);
        span.appendChild(document.createTextNode(textContent));

        return span;
    }

    /**
     * Wrap keywords in a line with span elements.
     * @param {string} line - The line of text to wrap.
     * @returns {DocumentFragment} The wrapped line as a document fragment.
     */
    static wrapKeywords(line) {
        const fragment = document.createDocumentFragment();
        const regex = new RegExp(`(\\b(?:${TraceSpan.keywords.join("|")})(?=\\W|$)|[()])`, 'g');

        let lastIndex = 0;
        let match;

        while ((match = regex.exec(line)) !== null) {
            const matchedText = match[0];

            if (match.index > lastIndex) {
                fragment.appendChild(document.createTextNode(line.slice(lastIndex, match.index)));
            }

            if (TraceSpan.keywords.includes(matchedText)) {
                fragment.appendChild(TraceSpan.createSpan(TraceSpanType.Keywords, matchedText));
            } else if (matchedText === '(' || matchedText === ')') {
                fragment.appendChild(TraceSpan.createSpan(TraceSpanType.Parenthesis, matchedText));
            }

            lastIndex = regex.lastIndex;
        }

        if (lastIndex < line.length) {
            fragment.appendChild(document.createTextNode(line.slice(lastIndex)));
        }

        return fragment;
    }

}
