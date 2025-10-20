import {TraceSpanType} from "./TraceSpanType.js";

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
     * Add color wrapping to line of code.
     * @param {string} line - The line of code to wrap.
     * @returns {DocumentFragment} The wrapped line as a document fragment.
     */
    static wrapLineColors(line) {
        const fragment = document.createDocumentFragment();

        // Escape keywords for safe use in a regex alternation.
        const keywordAlt = TraceSpan.keywords
            .map(k => k.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'))
            .join('|');

        const regex = new RegExp(
            `("(?:(?:\\\\.|[^"\\\\])*)")|(\\b(?:${keywordAlt})(?=\\W|$)|[()])`,
            'g'
        );

        let lastIndex = 0;
        let match;

        while ((match = regex.exec(line)) !== null) {
            const [full, stringLit] = match;

            if (match.index > lastIndex) {
                fragment.appendChild(
                    document.createTextNode(line.slice(lastIndex, match.index))
                );
            }

            if (stringLit) {
                fragment.appendChild(TraceSpan.createSpan(TraceSpanType.String, full));
            } else if (TraceSpan.keywords.includes(full)) {
                fragment.appendChild(TraceSpan.createSpan(TraceSpanType.Keywords, full));
            } else if (full === '(' || full === ')') {
                fragment.appendChild(TraceSpan.createSpan(TraceSpanType.Parenthesis, full));
            }

            lastIndex = regex.lastIndex;
        }

        if (lastIndex < line.length) {
            fragment.appendChild(document.createTextNode(line.slice(lastIndex)));
        }

        return fragment;
    }
}
