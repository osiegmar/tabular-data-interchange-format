# Tabular Data Interchange Format (TDIF)

Tabular Data Interchange Format (TDIF) is a lightweight and language-independent data interchange format for the
exchange of tabular data.

> [!WARNING]
> This document is currently in draft form. Do not implement it yet.

## Rationale

With formats like XML, JSON, YAML, CBOR and many others, there seems to be no shortage of standardized, hierarchical
data interchange and serialization formats.
For simple tabular data, however, there is a lack of standardized formats.

The closest thing to a standard for tabular data is CSV (comma-separated values) as described
in [RFC 4180](https://datatracker.ietf.org/doc/html/rfc4180).
Unfortunately, this RFC is only an informative, non-normative and thus nonbinding memo.
It does not strictly define the CSV format and leaves room for interpretation and implementation-specific behavior.
This is because CSV has been in use for decades before RFC 4180 was published.

Those differences can lead to unexpected, yet incorrect results. This is a continuous source of frustration for
developers and financial damage for organizations.

Typical differences between CSV implementations:

- File encoding (US-ASCII, UTF-8, UTF-16, UTF-32, ...)
- Use of a BOM (byte order mark) header
- End of line character (LF, CR, CRLF)
- Field separator (comma, semicolon, tab, ...)
- Quoting character (double-quote, single-quote, ...)
- Escaping of the quoting character (doubling, backslash, ...)
- Presence of a header record
- Uniqueness of header field names including case-sensitivity
- Use of comments
- When to quote a field
- Handling of empty lines
- Handling of empty fields
- Interpretation of null values
- Handling of whitespace characters
- Handling of leading and trailing whitespaces
- Handling of linebreaks within a field
- Handling of multibyte characters for field separator, quoting character, escape character, ...
- Handling of non-CSV data within the file (e.g., information headers, copyright, ...)

Many of these differences are incorporated in the DRAFT
of [RFC-4180-bis](https://datatracker.ietf.org/doc/html/draft-shafranovich-rfc4180-bis)
but only to describe the current state of CSV implementations and related concerns.
The main problem remains: The RFC will stay non-normative and thus nonbinding, and when people talk about CSV, they
will still have different things in mind.

TDIF, while similar to CSV, is a strict and unambiguous format.
Due to its similarity to CSV, it is not only straightforward to read and write, but also compatible with many existing
tools.

By definition, any TDIF file can be read from any existing RFC 4180 compliant CSV parser as long as no comments are used
or the CSV parser supports comments in the most common way currently found in CSV implementations 
(lines starting with a hash character (`#`)).

## Specification

### Glossary

- **TDIF file**: A file that contains a sequence of records and optionally comments.
- **record**: A sequence of fields.
- **header**: The first record in a TDIF file that contains the field names.
- **field**: A value within a record.
- **comment**: Lines in a TDIF file that are for commentary.

The term **TDIF file** is for descriptive purposes only. TDIF files can be stored in files, but they can also be
transmitted over the network or stored in a database. The term **TDIF file** is used throughout this document for
simplicity.

### Encoding, Content, and Line Endings

TDIF files MUST be encoded using the UTF-8 character encoding and MUST NOT include a BOM (byte order mark) header.

Fields and comments MAY contain binary data, including NUL characters.

Records and comments MUST be terminated by one of the following end-of-line characters:

- Line-feed character (LF)
- Carriage-return character (CR)
- Carriage-return character followed by a line-feed character (CRLF)

As the end-of-line characters are dependent on the operating system, the TDIF format does not specify any preference.
For simplicity, the rest of this document uses the term "linebreak" to refer to any of the above characters.

TDIF files MUST not contain any data outside of records and comments.
This includes empty lines that are not part of a multi-line field.
An "empty line" is defined as a sequence of linebreaks.

TDIF files MUST contain at least the header record.

### Records

A record consists of fields, separated by a comma (`,`) and is terminated by a linebreak.

The first record in a TDIF file MUST be a header record, which contains the names of the fields for the succeeding
records.
Field names MUST be unique (case-insensitive), and each field name MUST be enclosed in double quotation marks (`"`).
The header record MUST have at least one field.

Each record MUST have the same number of fields as the header record.

Fields MUST be either `\N` to denote a null value or they MUST be enclosed in double quotation marks (`"`).
Records MUST NOT contain empty fields (`,,`).
Whitespace characters outside a field MUST NOT occur (`"foo", "bar"`).
Double quotation marks within a field MUST be escaped by doubling it (`""`).

Field values may span multiple lines and may contain any character including binary data.

**Examples:**

The following example contains a header record and three data records with one field each.

```
"header"CRLF
"123"CRLF
"foo ""is"" bar"CRLF
\NCRLF
```

The first data record contains a field with the value `123`.
The second data record contains a field with the value `foo "is" bar`.
The quotation marks had to be escaped by doubling them.
The third data and last record contains a field with a null value.

The following example demonstrates the use of a multi-line field.

```
"first name","last name","address","country"CRLF
"John","Doe","123 Maple StreetLF
AnytownLF
PA 17101","US"CRLF
"Max","Mustermann",\N,"DE"CRLF
```

Note that the line-feed characters in the multi-line "address"-field are part of the field value and not record
separators.

### Comments

A line starting with a hash character (`#`) is considered a comment.
The comment is terminated by a linebreak.

The hash character MUST be the first character of the line.
Until the terminating linebreak, any character is allowed.

Comments MAY be inserted anywhere in the file, except within a record.

**Example:**

```
# This is a commentCRLF
"header1","header2","header3"CRLF
# This is another commentCRLF
"value1","value2","value3"CRLF
"# This is not a comment",\N,"# also not a comment"CRLF
# This is a third commentCRLF
```

### ABNF Grammar

The following shows the specification in ABNF (Augmented Backus-Naur Form).
In case of ambiguity, the ABNF grammar is the authoritative source of the specification and takes precedence over the
text.

```abnf
;;; file

file            = *comment header *(comment / record)

;;; header and record

header          = value *(comma value) linebreak

record          = field *(comma field) linebreak

field           = null / value

value           = DQUOTE *(textdata / 2DQUOTE) DQUOTE

textdata        = %x00-21 / %x23-7F / UTF8-data
                    ; all characters except quotation mark

;;; comment

comment         = hash *commentdata linebreak

commentdata     = %x00-09 / %x0B-0C / %x0E-7F / UTF8-data
                    ; all characters except LF, CR

;;; Common rules

null            = %x5C.4E       ; \N

comma           = %x2C          ; ,

hash            = %x23          ; #

linebreak       = CR / LF / CRLF

;;; Basic rules

LF              = %x0A
                    ; as per section B.1 of [RFC5234]

CR              = %x0D
                    ; as per section B.1 of [RFC5234]

CRLF            = CR LF
                    ; as per section B.1 of [RFC5234]

DQUOTE          = %x22
                    ; as per section B.1 of [RFC5234]

UTF8-data       = UTF8-2 / UTF8-3 / UTF8-4
                    ; as per section 4 of [RFC3629]
```

## Considerations

While TDIF aims to be as close to CSV as possible, there are some intentional differences. These differences are made
to circumvent the ambiguities of CSV and address some very often used features that sometimes lead to interchange
problems and unexpected results (see [Rationale section](#rationale)).

- Specify these features (mentioned but not specified in RFC 4180-bis)
    - Explicit null values
    - Comments
- Make the format unambiguous
    - Specify the file encoding
    - Make the header record mandatory and its fields unique
    - Require the same number of fields in each record
    - Allow only one way to separate fields
    - Specify the difference between null values and empty fields

These differences come with a few consequences:

- Empty lines are no longer necessary and thus not allowed
- Empty fields (`,,`) are no longer necessary/meaningful and thus not allowed
- Fields are always enclosed in double quotation marks

### Null values

TDIF defines `\N` for null values. This decision was made because other mechanisms, such as an empty unquoted field
(`,,`), are ambiguous. This is especially true for files that contain only one field per record.

In the following example, it's not clear if the file contains a null value in the third line.

```
header1CRLF
value1CRLF
```

In TDIF this is unambiguous:

```
"header1"CRLF
"value1"CRLF
\NCRLF
```

This approach is similar to that of the PostgreSQL database, which uses \N to represent null values in CSV files.

### Comments

Comments are not mentioned in RFC 4180. However, they are a common feature in CSV implementations. They are used to
provide additional information about the file like the author, the creation date, the source, usage instructions, etc.
The TDIF format specifies comments to make it easier to embed additional information in the file. In RFC 4180-bis,
comments are mentioned as a possible extension.

### Always enclose fields in double quotation marks

In CSV, fields need to be enclosed in double quotation marks only if they contain control characters such as a field
separator, a linebreak or a quotation mark.
In TDIF, this list has to be extended by the hash character (`#`) to support comments.

Still, there are situations where ambiguity arises. For example:

**Example 1**: Empty field in the last record of a file that contains only one field per record:

In the following example, it's not clear if the file contains an empty value in the third line.

```
header1CRLF
value1CRLF
```

In TDIF this is unambiguous:

```
"header1"CRLF
"value1"CRLF
""CRLF
```

RFC 4180-bis mentions this ambiguity and requires and end-of-line character event after the last field of the last
record. But which application uses the new and which the old rule?

**Example 2**: Desired whitespaces:

```
header1,_header2CRLF
_foo,bar_
```

Underscores are used to represent whitespaces to prevent the Markdown editor/renderer from removing them.

Which of the whitespaces are desired and which are not? This is not clear. The following example is unambiguous.

```
"header1"," header2"CRLF
" foo","bar "CRLF
```

For the sake of simplicity and clarity, TDIF requires that all fields are enclosed in double quotation marks. This makes
the format unambiguous and easy to read and write.

## Request for Comments

This document is currently in draft form. Comments and suggestions are welcome. Please start
a [discussion](https://github.com/osiegmar/tabular-data-interchange-format/discussions) or create a pull request.

## Implementation

See [implementations](implementations.md).

## License

This document is licensed under the [CC0 1.0 Universal (CC0 1.0) Public Domain Dedication](LICENSE) license.

The reference implementations in this repository are licensed under the [MIT](reference-impl/LICENSE) license.
