grammar TDIF;

file
    : comment* header (comment | record)* EOF
    ;

header
    : STRING (',' STRING)* linebreak
    ;

record
    : field (',' field)* linebreak
    ;

field
    : '\\N' | STRING
    ;

comment
    : COMMENT linebreak
    ;

linebreak
    : '\r'? '\n'
    | '\r'
    ;

STRING
    : '"' (~["] | '\\"' | '\\')* '"'
    ;

COMMENT
    : '#' ~[\r\n]*
    ;
