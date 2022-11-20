# JCoRe BioNLP Gold and Predicted Genes Merge AE

**Descriptor Path**:
```
de.julielab.jcore.ae.bionlpgenesmerger.desc.jcore-bionlpgold-pred-gene-merge-ae
```

Given the gold BioNLP ST gene mentions and other gene mentions - possibly from gene recognizer - merges the two different sources of genes. For simplicity, this component employs two different types to represent genes. The BioNLP ST reader uses the de.julielab.jcore.types.Gene type. The other genes should be realized with de.julielab.jcore.types.Protein annotations.



**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| param1 | UIMA-Type | Boolean | Boolean | Description |
| param2 | UIMA-Type | Boolean | Boolean | Description |

**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| param1 | Syntax-Description | `Example` |
| param2 | Syntax-Description | `Example` |

**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.types.TYPE |  | `+` |
| de.julielab.jcore.types.ace.TYPE | `+` |  |


[1] Some Literature?
