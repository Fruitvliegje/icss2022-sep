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

variable: variableName ASSIGNMENT_OPERATOR sum SEMICOLON # VariableAssignment;
variableName: CAPITAL_IDENT;

ruleset: selector OPEN_BRACE declaration* CLOSE_BRACE ;
selector: ID_IDENT     #IdSelector
        | CLASS_IDENT  #ClassSelector
        | LOWER_IDENT  #TagSelector
        ;
declaration:  property COLON sum SEMICOLON;
property:LOWER_IDENT;

sum : sum PLUS term  #AddOperation
    | sum MIN term   #SubtractOperation
    | term           #SingleTerm
    ;

term : term MUL factor #MultiplyOperation
     | factor          #SingleFactor
     ;
factor: value ;
value
    : PIXELSIZE        # PixelLiteral
    | PERCENTAGE       # PercentageLiteral
    | COLOR            # ColorLiteral
    | SCALAR           # ScalarLiteral
    | TRUE             # BoolLiteral
    | FALSE            # BoolLiteral
    | variableName     # VariableReference
    ;

// Enter: maak astnode, zet op stack
// Exit: haal astnode van stack, voeg toe als kind aan node op de stack

