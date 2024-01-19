# Tabular Data Interchange Format (TDIF)

Tabular Data Interchange Format (TDIF) is a lightweight, text-based, language-independent data interchange format for
the exchange of tabular data.

## Rationale

With formats like XML, JSON, YAML, CBOR and many others, there seems to be no shortage for standardized, hierarchical
data interchange and serialization formats.

For simple tabular data, however, there is no truly standardized format. There's CSV, for
which [RFC 4180](https://datatracker.ietf.org/doc/html/rfc4180) describes an informative, non-normative and thus
nonbinding specification. Because CSV has been used decades before RFC 4180 was published, many different
interpretations and implementations exists. Even implementations that strictly follow RFC 4180 often behave differently
in some aspects. This is because the specification offers many options and leaves room for interpretation.
Those differences can lead to unexpected, yet incorrect results. This is a continuous source of frustration for
developers and financial damage for organizations.

Typical differences between CSV implementations:

- File encoding (US-ASCII, UTF-8, UTF-16, UTF-32, ...)
- Use of a BOM (byte order mark) header
- Line ending style (LF, CR, CRLF)
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
- Handling of line breaks within a field
- Handling of multibyte characters for field separator, quoting character, escape character, ...
- Handling of non-CSV data within the file (e.g., information headers, copyright, ...)

Many of these differences are incorporated in the DRAFT
of [RFC-4180-bis](https://datatracker.ietf.org/doc/html/draft-shafranovich-rfc4180-bis). But the main problem remains:
The RFC will stay non-normative and thus nonbinding and when people talk about CSV, they will still have different
things in mind.

TDIF, while similar to CSV, is a strict and unambiguous format. Due to its similarity to CSV, it is not only easy to
read and write, but also compatible with many existing tools.

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

### Content, Encoding and Line Endings

TDIF files MAY contain binary data and MUST be encoded in UTF-8. A BOM (byte order mark) header or any other non-TDIF
data MUST NOT be used. Lines are terminated by an end-of-line character that is either a line-feed character (LF), a
carriage-return character (CR), or a carriage-return character followed by a line-feed character (CRLF).

### Structure

A record is made up of fields separated by a comma (`,`) and terminated by an end-of-line character.
Each record MUST contain the same number of fields as the header record.

Fields MUST be either `\N` to denote a null value or they MUST be enclosed in double quotation marks (`"`).
Double quotation marks within a field MUST be escaped using a reverse solidus (`\`). The reverse solidus itself MUST be
escaped by another reverse solidus. Field values may span multiple lines and may contain any character including
binary data.

The first record in a TDIF file MUST be a header record. The header record MUST contain the field names. The field names
MUST be unique (case-insensitive). Any field name MUST be enclosed in double-quotes (`"`).

Example:

```csv
"first name","last name","age"
"John","Doe",42
"Jane","Doe",\N
```

### Comments

A line starting with a hash character (`#`) is considered a comment. The hash character MUST be the first character of
the line. Until the end of line, any character is allowed. The end-of-line character(s) MUST NOT be part of the comment.
Comments MAY be inserted anywhere in the file, except within a record.

```csv
# This is a comment
"header1","header2","header3"
# This is another comment
"value1","value2","value3"
"# This is not a comment",\N,"# also not a comment"
# This is a third comment
```

### Miscellaneous

- There MUST NOT be any blank lines
- There MUST NOT be any empty fields
- There MUST NOT be any whitespace outside a field
- Whitespace characters within a field MUST NOT be ignored

### ABNF Grammar

```abnf
;;; file

file            = *comment header *(comment / record)

;;; header and record

header          = value *(comma value) linebreak

record          = field *(comma field) linebreak

field           = null / value

value           = DQUOTE *(textdata / escaped-quote / escaped-escaped) DQUOTE

textdata        = %x00-5B / %x5D-7F / UTF8-data
                    ; all characters except reverse solidus

;;; comment

comment         = hash *commentdata linebreak

commentdata     = %x00-09 / %x0B-0C / %x0E-7F / UTF8-data
                    ; all characters except LF, CR

;;; Common rules

null            = %x5C.4E       ; \N

escaped-quote   = %x5C.22       ; \"

escaped-escaped = %x5C.5C       ; \\

comma           = %x2C          ; ,

hash            = %x23          ; #

linebreak       = CR / LF / CRLF

;;; Basic rules

CR              = %x0D
                    ; as per section B.1 of [RFC5234]

CRLF            = CR LF
                    ; as per section B.1 of [RFC5234]

DQUOTE          = %x22
                    ; as per section B.1 of [RFC5234]

LF              = %x0A
                    ; as per section B.1 of [RFC5234]

UTF8-data       = UTF8-2 / UTF8-3 / UTF8-4
                    ; as per section 4 of [RFC3629]
```

## Considerations

### Null values

TDIF defines `\N` for null values. This decision was made because other mechanisms, such as an empty unquoted field
(`,,`), are ambiguous. This is especially true for files that contain only one field per record.

In the following example it's not clear if the file contains a null value in the third line.

```csv
header1CRLF
value1CRLF
```

In TDIF this is unambiguous, no matter if the last line is terminated by a line-feed character or not.

```csv
"header1"CRLF
"value1"CRLF
\N
```

This approach is similar to that of the PostgreSQL database, which uses \N to represent null values in CSV files.

## Request for Comments

This document is currently in draft form. Comments and suggestions are welcome. Please start
a [discussion](https://github.com/osiegmar/tabular-data-interchange-format/discussions) or create a pull request.

## Implementation

See [implementations](implementations.md).
