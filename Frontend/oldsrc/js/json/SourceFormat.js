export class SourceFormat {
    constructor(sourceFormat) {
        this.sourceFile = new SourceFile(sourceFormat.sourceFile);
        this.syntaxNodes = Object.values(sourceFormat.syntaxNodes).map(syntaxNode => new SyntaxNode(syntaxNode));
    }
}

class SourceFile {
    constructor(sourceFile) {
        this.fileName = sourceFile.fileName;
        this.packageName = sourceFile.packageName;
    }
}

class SyntaxNode {
    constructor(syntaxNode) {
        this.endLine = syntaxNode.endLine;
        this.identifier = syntaxNode.identifier;
        this.expression = syntaxNode.expression ? new Expression(syntaxNode.expression) : undefined;
        this.children = syntaxNode.children;
        this.kind = syntaxNode.kind;
        this.prefix = syntaxNode.prefix ? new Prefix(syntaxNode.prefix) : undefined;
        this.startLine = syntaxNode.startLine;
        this.suffix = syntaxNode.suffix ? new Suffix(syntaxNode.suffix) : undefined;
        this.sourceFile = syntaxNode.sourceFile ? new SourceFile(syntaxNode.sourceFile) : undefined;
    }

    getEntireText() {
        if (this.expression == null) {
            return "";
        }

        let prefixText = this.prefix.text;
        if (prefixText.startsWith("{") || prefixText.startsWith("}")) {
            prefixText = prefixText.substring(1).trimStart();
        }

        let suffixText = this.suffix.text;
        if (suffixText.endsWith("}") || suffixText.endsWith("{")) {
            suffixText = suffixText.substring(0, suffixText.length - 1).trimEnd();
        }

        let entireText = prefixText + this.getExpressionText() + suffixText;

        //Parenthesis
        let openCount = 0;
        for (let char of entireText) {
            if (char === '(') {
                openCount++;
            } else if (char === ')') {
                openCount = Math.max(openCount - 1, 0);
            }
        }
        entireText += ')'.repeat(openCount);
        return entireText;//.replace(/(\r\n|\n|\r)/gm, "");
    }

    getExpressionText() {
        if (!this.expression) {
            return "";
        }

        return this.expression.tokens.map(token => {
            if (token.text) return token.text.trim()
            return "";
        }).join("");
    }

    getFirstExpressionWithoutParenthesis() {
        if (!this.expression) {
            return "";
        }

        return this.expression.tokens[0].text.replace(/[()]+$/, "");
    }
}

class Expression {
    constructor(expression) {
        this.tokens = expression.tokens.map(token => new Token(token));
        this.kind = expression.kind;
    }
}

class Token {
    constructor(token) {
        this.kind = token.kind;
        this.text = token.text;
    }
}

class Prefix {
    constructor(prefix) {
        this.kind = prefix.kind;
        this.text = prefix.text.trimStart();
    }
}

class Suffix {
    constructor(suffix) {
        this.kind = suffix.kind
        this.text = suffix.text.trimEnd();
    }
}

class sourceFile {
    constructor(sourceFile) {
        this.fileName = sourceFile.fileName;
        this.packageName = sourceFile.packageName;
    }
}
