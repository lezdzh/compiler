grammar mxstar;
code:
	(classdef|funcdef|vardef)*
;
classdef:
	'class'Name'{'
		(funcdef|vardef)*
		constructfuncdef?
		(funcdef|vardef)*
	'}'
;
constructfuncdef:
	Name'('')''{'statement*'}'
;
funcdef:
	type Name'('(type Name(','type Name)*)?')''{'
		statement*
	'}'
;
vardef:
	type Name('='expr)?(','Name('='expr)?)*';'
;
type:
	('bool'|'int'|'string'|'void'|Name)('['']')*
;
statement:
	'{'statement*'}' #s1
	|';' #s2
	|vardef #s3
	|expr';' #s4
	|'break'';' #s5
	|'continue'';' #S6
	|'return'expr?';' #s7
	|'if''('expr')'statement('else'statement)? #s8
	|'while''('expr')'statement #s9
	|'for''('expr?';'expr?';'expr?')'statement #s10
;
expr:
	'this' #e0
	|Num #e1
	|'true' #e2
	|'false' #e3
	|String #e4
	|'null' #e5
	|Name #e6
	|expr'.''('?Name')'? #e8
	|'('expr')' #e7
	|expr'('(expr(','expr)*)?')' #e9
	|expr'['expr']' #e10
	|expr('++'|'--') #e11
	|('++'|'--'|'~'|'!'|'-'|'+')expr #e12
	|'new'(Name|'int'|'bool'|'string')(('['expr']')|('['']'))*('('')')? #e13
	|expr('*'|'/'|'%')expr #e14
	|expr('+'|'-')expr #e15
	|expr('<<'|'>>')expr #e16
	|expr('<'|'>'|'<='|'>=')expr #e17
	|expr('=='|'!=')expr #e18
	|expr'&'expr #e19
	|expr'^'expr #e20
	|expr'|'expr #e21
	|expr'&&'expr #e22
	|expr'||'expr #e23
	|expr'='expr #e24
;
Name:
	[a-zA-Z][a-zA-Z0-9_]*
;
Num:
	[0-9]+
;
String:
	'"'Char*'"'
;

fragment Char:
	~["\\\r\n]
	|'\\'["n\\r]
;
Comment:
	'//'~[\r\n]*->skip
;
Comment2:
	'/*'.*?'*/'->skip
;
Invisible
:
	(EOF|[ \t\n\r])->skip
;
