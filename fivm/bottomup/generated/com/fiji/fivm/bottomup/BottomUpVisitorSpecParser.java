// $ANTLR 3.2 Sep 23, 2009 12:02:23 com/fiji/fivm/bottomup/BottomUpVisitorSpec.g 2015-02-26 22:44:18

package com.fiji.fivm.bottomup;
import java.util.LinkedHashMap;


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

public class BottomUpVisitorSpecParser extends Parser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "NEWLINE", "ID", "WS", "COMMENT", "'class'", "'module'", "':='", "'include'", "'('", "','", "')'"
    };
    public static final int WS=6;
    public static final int NEWLINE=4;
    public static final int T__12=12;
    public static final int T__11=11;
    public static final int T__14=14;
    public static final int T__13=13;
    public static final int T__10=10;
    public static final int COMMENT=7;
    public static final int ID=5;
    public static final int EOF=-1;
    public static final int T__9=9;
    public static final int T__8=8;

    // delegates
    // delegators


        public BottomUpVisitorSpecParser(TokenStream input) {
            this(input, new RecognizerSharedState());
        }
        public BottomUpVisitorSpecParser(TokenStream input, RecognizerSharedState state) {
            super(input, state);
             
        }
        

    public String[] getTokenNames() { return BottomUpVisitorSpecParser.tokenNames; }
    public String getGrammarFileName() { return "com/fiji/fivm/bottomup/BottomUpVisitorSpec.g"; }


        public void emitErrorMessage(String msg) {
            throw new Error("Parse error: "+msg);
        }



    // $ANTLR start "spec"
    // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:18:1: spec returns [Spec spec] : ( NEWLINE )* ( 'class' a= ID NEWLINE | 'module' ID NEWLINE ) (b= ID ':=' prod NEWLINE | 'include' c= ID | NEWLINE )* ;
    public final Spec spec() throws RecognitionException {
        Spec spec = null;

        Token a=null;
        Token b=null;
        Token c=null;
        Production prod1 = null;


        try {
            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:18:26: ( ( NEWLINE )* ( 'class' a= ID NEWLINE | 'module' ID NEWLINE ) (b= ID ':=' prod NEWLINE | 'include' c= ID | NEWLINE )* )
            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:19:9: ( NEWLINE )* ( 'class' a= ID NEWLINE | 'module' ID NEWLINE ) (b= ID ':=' prod NEWLINE | 'include' c= ID | NEWLINE )*
            {
             spec =new Spec(); 
            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:20:9: ( NEWLINE )*
            loop1:
            do {
                int alt1=2;
                int LA1_0 = input.LA(1);

                if ( (LA1_0==NEWLINE) ) {
                    alt1=1;
                }


                switch (alt1) {
            	case 1 :
            	    // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:20:9: NEWLINE
            	    {
            	    match(input,NEWLINE,FOLLOW_NEWLINE_in_spec53); 

            	    }
            	    break;

            	default :
            	    break loop1;
                }
            } while (true);

            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:21:9: ( 'class' a= ID NEWLINE | 'module' ID NEWLINE )
            int alt2=2;
            int LA2_0 = input.LA(1);

            if ( (LA2_0==8) ) {
                alt2=1;
            }
            else if ( (LA2_0==9) ) {
                alt2=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 2, 0, input);

                throw nvae;
            }
            switch (alt2) {
                case 1 :
                    // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:21:10: 'class' a= ID NEWLINE
                    {
                    match(input,8,FOLLOW_8_in_spec65); 
                    a=(Token)match(input,ID,FOLLOW_ID_in_spec69); 
                    match(input,NEWLINE,FOLLOW_NEWLINE_in_spec71); 

                                    spec.fullClassname=(a!=null?a.getText():null);
                                

                    }
                    break;
                case 2 :
                    // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:24:10: 'module' ID NEWLINE
                    {
                    match(input,9,FOLLOW_9_in_spec86); 
                    match(input,ID,FOLLOW_ID_in_spec88); 
                    match(input,NEWLINE,FOLLOW_NEWLINE_in_spec90); 

                    }
                    break;

            }

            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:25:9: (b= ID ':=' prod NEWLINE | 'include' c= ID | NEWLINE )*
            loop3:
            do {
                int alt3=4;
                switch ( input.LA(1) ) {
                case ID:
                    {
                    alt3=1;
                    }
                    break;
                case 11:
                    {
                    alt3=2;
                    }
                    break;
                case NEWLINE:
                    {
                    alt3=3;
                    }
                    break;

                }

                switch (alt3) {
            	case 1 :
            	    // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:25:11: b= ID ':=' prod NEWLINE
            	    {
            	    b=(Token)match(input,ID,FOLLOW_ID_in_spec105); 
            	    match(input,10,FOLLOW_10_in_spec107); 
            	    pushFollow(FOLLOW_prod_in_spec109);
            	    prod1=prod();

            	    state._fsp--;


            	                    if (spec.prods.containsKey((b!=null?b.getText():null))) {
            	                        System.err.println("Duplicate name "+(b!=null?b.getText():null));
            	                        System.exit(1);
            	                    }
            	                    spec.prods.put((b!=null?b.getText():null),prod1);
            	                
            	    match(input,NEWLINE,FOLLOW_NEWLINE_in_spec113); 

            	    }
            	    break;
            	case 2 :
            	    // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:32:11: 'include' c= ID
            	    {
            	    match(input,11,FOLLOW_11_in_spec127); 
            	    c=(Token)match(input,ID,FOLLOW_ID_in_spec131); 

            	                    spec.include((c!=null?c.getText():null));
            	                

            	    }
            	    break;
            	case 3 :
            	    // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:35:11: NEWLINE
            	    {
            	    match(input,NEWLINE,FOLLOW_NEWLINE_in_spec147); 

            	    }
            	    break;

            	default :
            	    break loop3;
                }
            } while (true);


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return spec;
    }
    // $ANTLR end "spec"


    // $ANTLR start "prod"
    // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:38:1: prod returns [Production prod] : ID ( '(' (p1= prod ( ',' p2= prod )* )? ')' )? ;
    public final Production prod() throws RecognitionException {
        Production prod = null;

        Token ID2=null;
        Production p1 = null;

        Production p2 = null;


        try {
            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:38:32: ( ID ( '(' (p1= prod ( ',' p2= prod )* )? ')' )? )
            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:39:9: ID ( '(' (p1= prod ( ',' p2= prod )* )? ')' )?
            {
            ID2=(Token)match(input,ID,FOLLOW_ID_in_prod175); 
             prod =new Production((ID2!=null?ID2.getText():null)); 
            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:40:9: ( '(' (p1= prod ( ',' p2= prod )* )? ')' )?
            int alt6=2;
            int LA6_0 = input.LA(1);

            if ( (LA6_0==12) ) {
                alt6=1;
            }
            switch (alt6) {
                case 1 :
                    // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:40:11: '(' (p1= prod ( ',' p2= prod )* )? ')'
                    {
                    match(input,12,FOLLOW_12_in_prod189); 
                     prod.makeOperation(); 
                    // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:41:13: (p1= prod ( ',' p2= prod )* )?
                    int alt5=2;
                    int LA5_0 = input.LA(1);

                    if ( (LA5_0==ID) ) {
                        alt5=1;
                    }
                    switch (alt5) {
                        case 1 :
                            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:41:15: p1= prod ( ',' p2= prod )*
                            {
                            pushFollow(FOLLOW_prod_in_prod209);
                            p1=prod();

                            state._fsp--;

                             prod.addArg(p1); 
                            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:42:17: ( ',' p2= prod )*
                            loop4:
                            do {
                                int alt4=2;
                                int LA4_0 = input.LA(1);

                                if ( (LA4_0==13) ) {
                                    alt4=1;
                                }


                                switch (alt4) {
                            	case 1 :
                            	    // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:42:19: ',' p2= prod
                            	    {
                            	    match(input,13,FOLLOW_13_in_prod231); 
                            	    pushFollow(FOLLOW_prod_in_prod235);
                            	    p2=prod();

                            	    state._fsp--;

                            	     prod.addArg(p2); 

                            	    }
                            	    break;

                            	default :
                            	    break loop4;
                                }
                            } while (true);


                            }
                            break;

                    }

                    match(input,14,FOLLOW_14_in_prod244); 

                    }
                    break;

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return prod;
    }
    // $ANTLR end "prod"

    // Delegated rules


 

    public static final BitSet FOLLOW_NEWLINE_in_spec53 = new BitSet(new long[]{0x0000000000000310L});
    public static final BitSet FOLLOW_8_in_spec65 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_ID_in_spec69 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_NEWLINE_in_spec71 = new BitSet(new long[]{0x0000000000000832L});
    public static final BitSet FOLLOW_9_in_spec86 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_ID_in_spec88 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_NEWLINE_in_spec90 = new BitSet(new long[]{0x0000000000000832L});
    public static final BitSet FOLLOW_ID_in_spec105 = new BitSet(new long[]{0x0000000000000400L});
    public static final BitSet FOLLOW_10_in_spec107 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_prod_in_spec109 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_NEWLINE_in_spec113 = new BitSet(new long[]{0x0000000000000832L});
    public static final BitSet FOLLOW_11_in_spec127 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_ID_in_spec131 = new BitSet(new long[]{0x0000000000000832L});
    public static final BitSet FOLLOW_NEWLINE_in_spec147 = new BitSet(new long[]{0x0000000000000832L});
    public static final BitSet FOLLOW_ID_in_prod175 = new BitSet(new long[]{0x0000000000001002L});
    public static final BitSet FOLLOW_12_in_prod189 = new BitSet(new long[]{0x0000000000004020L});
    public static final BitSet FOLLOW_prod_in_prod209 = new BitSet(new long[]{0x0000000000006000L});
    public static final BitSet FOLLOW_13_in_prod231 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_prod_in_prod235 = new BitSet(new long[]{0x0000000000006000L});
    public static final BitSet FOLLOW_14_in_prod244 = new BitSet(new long[]{0x0000000000000002L});

}