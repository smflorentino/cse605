grammar BottomUpVisitorSpec;

@header {
package com.fiji.fivm.bottomup;
import java.util.LinkedHashMap;
}

@lexer::header {
package com.fiji.fivm.bottomup;
}

@members {
    public void emitErrorMessage(String msg) {
        throw new Error("Parse error: "+msg);
    }
}

spec returns [Spec spec] :
        { $spec=new Spec(); }
        NEWLINE*
        ('class' a=ID NEWLINE {
                $spec.fullClassname=$a.text;
            } |
         'module' ID NEWLINE)
        ( b=ID ':=' prod {
                if ($spec.prods.containsKey($b.text)) {
                    System.err.println("Duplicate name "+$b.text);
                    System.exit(1);
                }
                $spec.prods.put($b.text,$prod.prod);
            } NEWLINE |
          'include' c=ID {
                $spec.include($c.text);
            } |
          NEWLINE )*
    ;

prod returns [Production prod] :
        ID { $prod=new Production($ID.text); }
        ( '(' { $prod.makeOperation(); }
            ( p1=prod { $prod.addArg($p1.prod); }
                ( ',' p2=prod { $prod.addArg($p2.prod); } )* )?')' )?
    ;

ID      : (('!')?('$'|'%'))?('a'..'z'|'A'..'Z'|'0'..'9'|'_'|'.'|'-')* ;
NEWLINE : '\r'?'\n';
WS      : (' '|'\t')+ {skip();} ;
COMMENT : '#' ~('\n'|'\r')* ('\r'? '\n')? {$channel=HIDDEN;} ;

