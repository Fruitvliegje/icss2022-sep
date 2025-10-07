grammar ICSS;

//--- LEXER: ---

// IF support:
IF: 'if';
ELSE: 'else';
BOX_BRACKET_OPEN: '[';
BOX_BRACKET_CLOSE: ']';


//Literals
TRUE: 'TRUE';
FALSE: 'FALSE';
PIXELSIZE: [0-9]+ 'px';
PERCENTAGE: [0-9]+ '%';
SCALAR: [0-9]+;


//Color value takes precedence over id idents
COLOR: '#' [0-9a-f] [0-9a-f] [0-9a-f] [0-9a-f] [0-9a-f] [0-9a-f];

//Specific identifiers for id's and css classes
ID_IDENT: '#' [a-z0-9\-]+;
CLASS_IDENT: '.' [a-z0-9\-]+;

//General identifiers
LOWER_IDENT: [a-z] [a-z0-9\-]*;
CAPITAL_IDENT: [A-Z] [A-Za-z0-9_]*;

//All whitespace is skipped
WS: [ \t\r\n]+ -> skip;

//
OPEN_BRACE: '{';
CLOSE_BRACE: '}';
SEMICOLON: ';';
COLON: ':';
PLUS: '+';
MIN: '-';
MUL: '*';
ASSIGNMENT_OPERATOR: ':=';

// ? Optioneel: 0 of 1 keer
// * 0 of meer keer
// + 1 of meer
// ('a' | 'b') 'c'; a of b anders altijd c



//--- PARSER: ---



stylesheet: variable* ruleset* ;

variable: variableName ASSIGNMENT_OPERATOR sum SEMICOLON;
variableName: CAPITAL_IDENT;

ruleset: selector OPEN_BRACE declaration* CLOSE_BRACE ;
selector: ID_IDENT | CLASS_IDENT | LOWER_IDENT;
declaration: LOWER_IDENT COLON sum SEMICOLON;
value:PIXELSIZE | PERCENTAGE | COLOR | SCALAR | TRUE| FALSE | variableName;
sum : sum MUL value | sum PLUS value | sum MIN value | value;





