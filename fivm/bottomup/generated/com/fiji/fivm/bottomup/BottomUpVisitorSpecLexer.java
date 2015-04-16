// $ANTLR 3.2 Sep 23, 2009 12:02:23 com/fiji/fivm/bottomup/BottomUpVisitorSpec.g 2015-04-15 21:29:55

package com.fiji.fivm.bottomup;


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

public class BottomUpVisitorSpecLexer extends Lexer {
    public static final int WS=6;
    public static final int NEWLINE=4;
    public static final int T__12=12;
    public static final int T__11=11;
    public static final int T__14=14;
    public static final int T__13=13;
    public static final int T__10=10;
    public static final int ID=5;
    public static final int COMMENT=7;
    public static final int EOF=-1;
    public static final int T__9=9;
    public static final int T__8=8;

    // delegates
    // delegators

    public BottomUpVisitorSpecLexer() {;} 
    public BottomUpVisitorSpecLexer(CharStream input) {
        this(input, new RecognizerSharedState());
    }
    public BottomUpVisitorSpecLexer(CharStream input, RecognizerSharedState state) {
        super(input,state);

    }
    public String getGrammarFileName() { return "com/fiji/fivm/bottomup/BottomUpVisitorSpec.g"; }

    // $ANTLR start "T__8"
    public final void mT__8() throws RecognitionException {
        try {
            int _type = T__8;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:7:6: ( 'class' )
            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:7:8: 'class'
            {
            match("class"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__8"

    // $ANTLR start "T__9"
    public final void mT__9() throws RecognitionException {
        try {
            int _type = T__9;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:8:6: ( 'module' )
            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:8:8: 'module'
            {
            match("module"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__9"

    // $ANTLR start "T__10"
    public final void mT__10() throws RecognitionException {
        try {
            int _type = T__10;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:9:7: ( ':=' )
            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:9:9: ':='
            {
            match(":="); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__10"

    // $ANTLR start "T__11"
    public final void mT__11() throws RecognitionException {
        try {
            int _type = T__11;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:10:7: ( 'include' )
            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:10:9: 'include'
            {
            match("include"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__11"

    // $ANTLR start "T__12"
    public final void mT__12() throws RecognitionException {
        try {
            int _type = T__12;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:11:7: ( '(' )
            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:11:9: '('
            {
            match('('); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__12"

    // $ANTLR start "T__13"
    public final void mT__13() throws RecognitionException {
        try {
            int _type = T__13;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:12:7: ( ',' )
            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:12:9: ','
            {
            match(','); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__13"

    // $ANTLR start "T__14"
    public final void mT__14() throws RecognitionException {
        try {
            int _type = T__14;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:13:7: ( ')' )
            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:13:9: ')'
            {
            match(')'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__14"

    // $ANTLR start "ID"
    public final void mID() throws RecognitionException {
        try {
            int _type = ID;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:45:9: ( ( ( '!' )? ( '$' | '%' ) )? ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_' | '.' | '-' )* )
            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:45:11: ( ( '!' )? ( '$' | '%' ) )? ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_' | '.' | '-' )*
            {
            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:45:11: ( ( '!' )? ( '$' | '%' ) )?
            int alt2=2;
            int LA2_0 = input.LA(1);

            if ( (LA2_0=='!'||(LA2_0>='$' && LA2_0<='%')) ) {
                alt2=1;
            }
            switch (alt2) {
                case 1 :
                    // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:45:12: ( '!' )? ( '$' | '%' )
                    {
                    // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:45:12: ( '!' )?
                    int alt1=2;
                    int LA1_0 = input.LA(1);

                    if ( (LA1_0=='!') ) {
                        alt1=1;
                    }
                    switch (alt1) {
                        case 1 :
                            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:45:13: '!'
                            {
                            match('!'); 

                            }
                            break;

                    }

                    if ( (input.LA(1)>='$' && input.LA(1)<='%') ) {
                        input.consume();

                    }
                    else {
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}


                    }
                    break;

            }

            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:45:29: ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_' | '.' | '-' )*
            loop3:
            do {
                int alt3=2;
                int LA3_0 = input.LA(1);

                if ( ((LA3_0>='-' && LA3_0<='.')||(LA3_0>='0' && LA3_0<='9')||(LA3_0>='A' && LA3_0<='Z')||LA3_0=='_'||(LA3_0>='a' && LA3_0<='z')) ) {
                    alt3=1;
                }


                switch (alt3) {
            	case 1 :
            	    // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:
            	    {
            	    if ( (input.LA(1)>='-' && input.LA(1)<='.')||(input.LA(1)>='0' && input.LA(1)<='9')||(input.LA(1)>='A' && input.LA(1)<='Z')||input.LA(1)=='_'||(input.LA(1)>='a' && input.LA(1)<='z') ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;}


            	    }
            	    break;

            	default :
            	    break loop3;
                }
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ID"

    // $ANTLR start "NEWLINE"
    public final void mNEWLINE() throws RecognitionException {
        try {
            int _type = NEWLINE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:46:9: ( ( '\\r' )? '\\n' )
            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:46:11: ( '\\r' )? '\\n'
            {
            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:46:11: ( '\\r' )?
            int alt4=2;
            int LA4_0 = input.LA(1);

            if ( (LA4_0=='\r') ) {
                alt4=1;
            }
            switch (alt4) {
                case 1 :
                    // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:46:11: '\\r'
                    {
                    match('\r'); 

                    }
                    break;

            }

            match('\n'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "NEWLINE"

    // $ANTLR start "WS"
    public final void mWS() throws RecognitionException {
        try {
            int _type = WS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:47:9: ( ( ' ' | '\\t' )+ )
            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:47:11: ( ' ' | '\\t' )+
            {
            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:47:11: ( ' ' | '\\t' )+
            int cnt5=0;
            loop5:
            do {
                int alt5=2;
                int LA5_0 = input.LA(1);

                if ( (LA5_0=='\t'||LA5_0==' ') ) {
                    alt5=1;
                }


                switch (alt5) {
            	case 1 :
            	    // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:
            	    {
            	    if ( input.LA(1)=='\t'||input.LA(1)==' ' ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;}


            	    }
            	    break;

            	default :
            	    if ( cnt5 >= 1 ) break loop5;
                        EarlyExitException eee =
                            new EarlyExitException(5, input);
                        throw eee;
                }
                cnt5++;
            } while (true);

            skip();

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "WS"

    // $ANTLR start "COMMENT"
    public final void mCOMMENT() throws RecognitionException {
        try {
            int _type = COMMENT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:48:9: ( '#' (~ ( '\\n' | '\\r' ) )* ( ( '\\r' )? '\\n' )? )
            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:48:11: '#' (~ ( '\\n' | '\\r' ) )* ( ( '\\r' )? '\\n' )?
            {
            match('#'); 
            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:48:15: (~ ( '\\n' | '\\r' ) )*
            loop6:
            do {
                int alt6=2;
                int LA6_0 = input.LA(1);

                if ( ((LA6_0>='\u0000' && LA6_0<='\t')||(LA6_0>='\u000B' && LA6_0<='\f')||(LA6_0>='\u000E' && LA6_0<='\uFFFF')) ) {
                    alt6=1;
                }


                switch (alt6) {
            	case 1 :
            	    // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:48:15: ~ ( '\\n' | '\\r' )
            	    {
            	    if ( (input.LA(1)>='\u0000' && input.LA(1)<='\t')||(input.LA(1)>='\u000B' && input.LA(1)<='\f')||(input.LA(1)>='\u000E' && input.LA(1)<='\uFFFF') ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;}


            	    }
            	    break;

            	default :
            	    break loop6;
                }
            } while (true);

            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:48:29: ( ( '\\r' )? '\\n' )?
            int alt8=2;
            int LA8_0 = input.LA(1);

            if ( (LA8_0=='\n'||LA8_0=='\r') ) {
                alt8=1;
            }
            switch (alt8) {
                case 1 :
                    // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:48:30: ( '\\r' )? '\\n'
                    {
                    // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:48:30: ( '\\r' )?
                    int alt7=2;
                    int LA7_0 = input.LA(1);

                    if ( (LA7_0=='\r') ) {
                        alt7=1;
                    }
                    switch (alt7) {
                        case 1 :
                            // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:48:30: '\\r'
                            {
                            match('\r'); 

                            }
                            break;

                    }

                    match('\n'); 

                    }
                    break;

            }

            _channel=HIDDEN;

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "COMMENT"

    public void mTokens() throws RecognitionException {
        // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:1:8: ( T__8 | T__9 | T__10 | T__11 | T__12 | T__13 | T__14 | ID | NEWLINE | WS | COMMENT )
        int alt9=11;
        alt9 = dfa9.predict(input);
        switch (alt9) {
            case 1 :
                // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:1:10: T__8
                {
                mT__8(); 

                }
                break;
            case 2 :
                // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:1:15: T__9
                {
                mT__9(); 

                }
                break;
            case 3 :
                // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:1:20: T__10
                {
                mT__10(); 

                }
                break;
            case 4 :
                // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:1:26: T__11
                {
                mT__11(); 

                }
                break;
            case 5 :
                // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:1:32: T__12
                {
                mT__12(); 

                }
                break;
            case 6 :
                // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:1:38: T__13
                {
                mT__13(); 

                }
                break;
            case 7 :
                // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:1:44: T__14
                {
                mT__14(); 

                }
                break;
            case 8 :
                // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:1:50: ID
                {
                mID(); 

                }
                break;
            case 9 :
                // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:1:53: NEWLINE
                {
                mNEWLINE(); 

                }
                break;
            case 10 :
                // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:1:61: WS
                {
                mWS(); 

                }
                break;
            case 11 :
                // com/fiji/fivm/bottomup/BottomUpVisitorSpec.g:1:64: COMMENT
                {
                mCOMMENT(); 

                }
                break;

        }

    }


    protected DFA9 dfa9 = new DFA9(this);
    static final String DFA9_eotS =
        "\3\10\1\uffff\1\10\7\uffff\11\10\1\30\2\10\1\uffff\1\33\1\10\1\uffff"+
        "\1\35\1\uffff";
    static final String DFA9_eofS =
        "\36\uffff";
    static final String DFA9_minS =
        "\1\11\1\154\1\157\1\uffff\1\156\7\uffff\1\141\1\144\1\143\1\163"+
        "\1\165\1\154\1\163\1\154\1\165\1\55\1\145\1\144\1\uffff\1\55\1\145"+
        "\1\uffff\1\55\1\uffff";
    static final String DFA9_maxS =
        "\1\155\1\154\1\157\1\uffff\1\156\7\uffff\1\141\1\144\1\143\1\163"+
        "\1\165\1\154\1\163\1\154\1\165\1\172\1\145\1\144\1\uffff\1\172\1"+
        "\145\1\uffff\1\172\1\uffff";
    static final String DFA9_acceptS =
        "\3\uffff\1\3\1\uffff\1\5\1\6\1\7\1\10\1\11\1\12\1\13\14\uffff\1"+
        "\1\2\uffff\1\2\1\uffff\1\4";
    static final String DFA9_specialS =
        "\36\uffff}>";
    static final String[] DFA9_transitionS = {
            "\1\12\1\11\2\uffff\1\11\22\uffff\1\12\2\uffff\1\13\4\uffff\1"+
            "\5\1\7\2\uffff\1\6\15\uffff\1\3\50\uffff\1\1\5\uffff\1\4\3\uffff"+
            "\1\2",
            "\1\14",
            "\1\15",
            "",
            "\1\16",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "\1\17",
            "\1\20",
            "\1\21",
            "\1\22",
            "\1\23",
            "\1\24",
            "\1\25",
            "\1\26",
            "\1\27",
            "\2\10\1\uffff\12\10\7\uffff\32\10\4\uffff\1\10\1\uffff\32\10",
            "\1\31",
            "\1\32",
            "",
            "\2\10\1\uffff\12\10\7\uffff\32\10\4\uffff\1\10\1\uffff\32\10",
            "\1\34",
            "",
            "\2\10\1\uffff\12\10\7\uffff\32\10\4\uffff\1\10\1\uffff\32\10",
            ""
    };

    static final short[] DFA9_eot = DFA.unpackEncodedString(DFA9_eotS);
    static final short[] DFA9_eof = DFA.unpackEncodedString(DFA9_eofS);
    static final char[] DFA9_min = DFA.unpackEncodedStringToUnsignedChars(DFA9_minS);
    static final char[] DFA9_max = DFA.unpackEncodedStringToUnsignedChars(DFA9_maxS);
    static final short[] DFA9_accept = DFA.unpackEncodedString(DFA9_acceptS);
    static final short[] DFA9_special = DFA.unpackEncodedString(DFA9_specialS);
    static final short[][] DFA9_transition;

    static {
        int numStates = DFA9_transitionS.length;
        DFA9_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA9_transition[i] = DFA.unpackEncodedString(DFA9_transitionS[i]);
        }
    }

    class DFA9 extends DFA {

        public DFA9(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 9;
            this.eot = DFA9_eot;
            this.eof = DFA9_eof;
            this.min = DFA9_min;
            this.max = DFA9_max;
            this.accept = DFA9_accept;
            this.special = DFA9_special;
            this.transition = DFA9_transition;
        }
        public String getDescription() {
            return "1:1: Tokens : ( T__8 | T__9 | T__10 | T__11 | T__12 | T__13 | T__14 | ID | NEWLINE | WS | COMMENT );";
        }
    }
 

}