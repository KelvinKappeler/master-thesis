import {ObjectValue, Value} from "../Def.js";

export class ObjectData {
    constructor(objectData) {
        this.objectData = objectData.map(objectDataNode => new ObjectDataNode(objectDataNode));
        this.stateDictionary = this.createStateDictionary();
    }

    getLastVersion(id, version) {
        const filteredNodes = this.objectData
            .filter(node => node.self.pointer === id && node.self.version <= version);

        if (filteredNodes.length === 0) return null;

        return filteredNodes.reduce((latestNode, currentNode) =>
            currentNode.self.version > latestNode.self.version ? currentNode : latestNode
        );
    }

    createStateDictionary() {
        const instanceRefs = this.objectData.filter(o => o.self.dataType === "instanceRef");
        const uniquePointers = new Set(instanceRefs.map(node => node.self.pointer));
        const stateDictionary = new Map();

        for (const pointer of uniquePointers) {
            let objectStates = this.objectData.filter(node => node.self.pointer === pointer);
            objectStates.sort((a, b) => a.self.version - b.self.version);
            objectStates = objectStates.map(node => new ObjectValue(node.self.className.className, node.self.pointer, node.self.version, Value.newFieldsValue(node.fields, this), this));
            stateDictionary.set(pointer, objectStates);
        }

        return stateDictionary;
    }
}

class ObjectDataNode {
    constructor(objectDataNode) {
        this.self = new Self(objectDataNode.self);
        this.fields = objectDataNode.fields ? objectDataNode.fields.map(f => new Field(f)) : undefined;
        this.values = objectDataNode.values ? new Values(objectDataNode.values) : undefined;
    }
}

class Self {
    constructor(self) {
        this.pointer = self.pointer;
        this.className = self.className ? new ClassName(self.className) : undefined;
        this.version = self.version;
        this.dataType = self.dataType;
        this.elemType = self.elemType;
    }
}

class Values {
    constructor(values) {
        this.values = values.map(v => new ArrayValue(v));
    }
}

class ArrayValue {
    constructor(arrayValue) {
        this.dataType = arrayValue.dataType;
        this.pointer = arrayValue.pointer;
        this.version = arrayValue.version;
        this.value = arrayValue.value;
        this.className = arrayValue.className ? new ClassName(arrayValue.className) : undefined;
    }
}

class Field {
    constructor(field) {
        this.identifier = new Identifier(field.identifier);
        this.value = field.value;
    }
}

class Identifier {
    constructor(identifier) {
        this.name = identifier.name;
        this.owner = identifier.owner;
        this.datatype = identifier.dataType;
    }
}

class ClassName {
    constructor(className) {
        this.className = className.className;
        this.packageName = className.packageName;
    }
}
