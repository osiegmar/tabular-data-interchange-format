# Tabular Data Interchange Format (TDIF)

Tabular Data Interchange Format (TDIF) is a lightweight, text-based, language-independent data interchange format for
the exchange of tabular data.

> [!WARNING]
> This document is currently in draft form. Do not implement it yet.

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
"John","Doe","42"
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

While TDIF aims to be as close to CSV as possible, there are some intentional differences. These differences are made
to circumvent the ambiguities of CSV and address some very often used features that sometimes lead to interchange
problems and unexpected results (see [Rationale section](#Rationale)).

- Specify these features (mentioned, but not specified in RFC 4180-bis)
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
- Always enclose fields in double quotation marks
- Empty fields (`,,`) are no longer necessary/meaningful and thus not allowed

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

### Comments

Comments are not mentioned in RFC 4180. However, they are a common feature in CSV implementations. They are used to
provide additional information about the file like the author, the creation date, the source, usage instructions, etc.
The TDIF format specifies comments to make it easier to embed additional information in the file. In RFC 4180-bis,
comments are mentioned as a possible extension.

### Always enclose fields in double quotation marks

In CSV, fields need to be enclosed in double quotation marks only if they contain control characters such as a field
separator, a line break or a quotation mark. In TDIF, this list has to be extended by the hash character (`#`) in order
to support comments.

Still, there are situations where ambiguity arises. For example:

**Example 1**: Empty field in the last record of a file that contains only one field per record:

In the following example it's not clear if the file contains an empty value in the third line.

```csv
header1CRLF
value1CRLF
```

In TDIF this is unambiguous, no matter if the last line is terminated by a line-feed character or not.

```csv
"header1"CRLF
"value1"CRLF
""
```

RFC 4180-bis mentions this ambiguity and requires and end-of-line character event after the last field of the last
record. But which application uses the new and which the old rule?

**Example 2**: Desired whitespaces:

```csv
header1,_header2CRLF
_foo,bar_
```

Underscores are used to represent whitespaces in order to prevent the Markdown editor/renderer from removing them.

Which of the whitespaces are desired and which are not? This is not clear. The following example is unambiguous.

```csv
"header1"," header2"CRLF
" foo","bar "
```

For the sake of simplicity and clarity, TDIF requires that all fields are enclosed in double quotation marks. This makes
the format unambiguous and easy to read and write. As a consequence, any whitespace character outside quotation marks
is not allowed.

### Escape quotation marks

> [!CAUTION]
> This section needs further discussion.

Current considerations why escaping of the quotation mark using a reverse solidus (`\`) instead of doubling it:

- The reverse solidus is already used for null values (`\N`).
- The reverse solidus is used for escaping in many other formats (e.g., JSON, YAML, XML, ...) whereas doubling does not
  seem to be used in any other format.
- Many CSV implementations already allow the reverse solidus as an alternative for escaping.
- It's easier to parse as the parser does not need to keep track of the previous character. A reverse solidus always
  escapes the next character where a quotation mark could be the end of the field or the escape character for the next
  quotation mark.
- Doubling the quotation mark is not intuitive and not easy to read. It does not seem to provide any advantage over
  escaping using a reverse solidus – except for being the current standard.
- The reverse solidus could be used for even more escaping (e.g., `\t` for tab, `\r` for carriage return, `\n` for line
  feed, ...). If newline characters were escaped, a record would always be on one line. This might increase readability
  for humans and could help with parsing (readline -> complete comment or record) and debugging.

Especially the last point (escaping CR and LF) is interesting. Given the following example:

```csv
"header"CRLF
"a multilineLF
value"CRLF
```

The LF in the second line is part of the value. The value may come from a database export for example, and it might
be important to preserve exactly as it is. It's easy to misinterpret a character on this position as a record separator.
A lot more unambiguity could be achieved by escaping CR and LF.

```csv
"header"CRLF
"a multiline\nvalue"CRLF
```

## Request for Comments

This document is currently in draft form. Comments and suggestions are welcome. Please start
a [discussion](https://github.com/osiegmar/tabular-data-interchange-format/discussions) or create a pull request.

## Implementation

See [implementations](implementations.md).
