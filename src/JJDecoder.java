import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;

public class JJDecoder {

    private final String encodedStr;

    public JJDecoder(String encodedStr) {
        this.encodedStr = clean(encodedStr);
    }

    public static void main(String[] args) throws ParseException {

        if (args.length > 0) {

            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(args[0]));

                StringBuilder text = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null)
                    text.append(line);

                JJDecoder decoder = new JJDecoder(text.toString());
                System.out.println(decoder.decode());

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (br != null) br.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }

        } else
            System.out.println("Usage: JJDecoder <jjencoded_file>");

    }

    public String decode() throws ParseException {
        ResultCheckPalindrome r = checkPalindrome(encodedStr);

        final String gv = r.gv;

        String data = encodedStr.substring(r.startPos, r.endPos);

        final String[] b = {"___+", "__$+", "_$_+", "_$$+", "$__+", "$_$+", "$$_+", "$$$+", "$___+", "$__$+", "$_$_+", "$_$$+", "$$__+", "$$_$+", "$$$_+", "$$$$+"};

        final String strL = "(![]+\"\")[" + gv + "._$_]+";
        final String strO = gv + "._$+";
        final String strT = gv + ".__+";
        final String strU = gv + "._+";

        final String strHex = gv + ".";

        final String strS = "\"";
        final String gvSig = gv + ".";

        final String strQuote = "\\\\\\\"";
        final String strSlash = "\\\\\\\\";

        final String strLower = "\\\\\"+";
        final String strUpper = "\\\\\"+" + gv + "._+";

        final String strEnd = "\"+";

        StringBuilder out = new StringBuilder();

        while (!data.isEmpty()) {
            // l o t u
            if (data.indexOf(strL) == 0) {
                data = data.substring(strL.length());
                out.append('l');
                continue;
            } else if (data.indexOf(strO) == 0) {
                data = data.substring(strO.length());
                out.append('o');
                continue;
            } else if (data.indexOf(strT) == 0) {
                data = data.substring(strT.length());
                out.append('t');
                continue;
            } else if (data.indexOf(strU) == 0) {
                data = data.substring(strU.length());
                out.append('u');
                continue;
            }

            // 0123456789abcdef
            if (data.indexOf(strHex) == 0) {
                data = data.substring(strHex.length());

                for (int i = 0; i < b.length; i++)
                    if (data.indexOf(b[i]) == 0) {
                        data = data.substring(b[i].length());
                        out.append(Integer.toHexString(i));
                        break;
                    }

                continue;
            }


            // start of s block
            if (data.indexOf(strS) == 0) {
                data = data.substring(strS.length());

                //check if "R
                if (data.indexOf(strUpper) == 0) { // r4 n >= 128
                    data = data.substring(strUpper.length()); // skip sig
                    String ch_str = "";
                    for (int i = 0; i < 2; i++) // # shouldn't be more than 2 hex chars
                        // gv + "."+b[ c ]
                        if (data.indexOf(gvSig) == 0) {
                            data = data.substring(gvSig.length());
                            for (int k = 0; k < b.length; k++) //# for every entry in b
                                if (data.indexOf(b[k]) == 0) {
                                    data = data.substring(b[k].length());
                                    ch_str = Integer.toHexString(k);
                                    break;
                                }
                        } else
                            break;

                    out.append((char) Integer.parseInt(ch_str, 16));
                    continue;
                } else if (data.indexOf(strLower) == 0) { //r3 check if "R // n < 128
                    data = data.substring(strLower.length()); // skip sig

                    String ch_str = "";
                    String ch_lotux = "";
                    String temp;
                    int b_checkR1 = 0;
                    for (int j = 0; j < 3; j++) { // shouldn't be more than 3 octal chars
                        if (j > 1) { // lotu check
                            if (data.indexOf(strL) == 0) {
                                data = data.substring(strL.length());
                                ch_lotux = "l";
                                break;
                            } else if (data.indexOf(strO) == 0) {
                                data = data.substring(strO.length());
                                ch_lotux = "o";
                                break;
                            } else if (data.indexOf(strT) == 0) {
                                data = data.substring(strT.length());
                                ch_lotux = "t";
                                break;
                            } else if (data.indexOf(strU) == 0) {
                                data = data.substring(strU.length());
                                ch_lotux = "u";
                                break;
                            }
                        }


                        // gv + "."+b[ c ]
                        if (data.indexOf(gvSig) == 0) {
                            temp = data.substring(gvSig.length());
                            for (int k = 0; k < 8; k++) // for every entry in b octal
                                if (temp.indexOf(b[k]) == 0) {
                                    if (Integer.parseInt(ch_str + k, 8) > 128) {
                                        b_checkR1 = 1;
                                        break;
                                    }

                                    ch_str += k;
                                    data = data.substring(gvSig.length()); // skip gvsig
                                    data = data.substring(b[k].length());
                                    break;
                                }


                            if (b_checkR1 == 1)
                                if (data.indexOf(strHex) == 0) { // 0123456789abcdef
                                    data = data.substring(strHex.length());
                                    // check every element of hex decode string for a match
                                    for (int i = 0; i < b.length; i++)
                                        if (data.indexOf(b[i]) == 0) {
                                            data = data.substring(b[i].length());
                                            ch_lotux = Integer.toHexString(i);
                                            break;
                                        }
                                    break;
                                }
                        } else
                            break;
                    }

                    out.append((char) Integer.parseInt(ch_str, 8));
                    out.append(ch_lotux);
                    continue;

                } else { //"S ----> "SR or "S+

                    int match = 0;
                    int n;

                    //# searching for matching pure s block
                    while (true) {
                        n = data.charAt(0);
                        if (data.indexOf(strQuote) == 0) {
                            data = data.substring(strQuote.length());
                            out.append('"');
                            match += 1;
                        } else if (data.indexOf(strSlash) == 0) {
                            data = data.substring(strSlash.length());
                            out.append('\\');
                            match += 1;
                        } else if (data.indexOf(strEnd) == 0) { // # reached end off S block ? +
                            if (match == 0)
                                throw new ParseException("No match S block: " + data, 0);
                            data = data.substring(strEnd.length());
                            break; //# step out of the while loop
                        } else if (data.indexOf(strUpper) == 0) { // # r4 reached end off S block ? - check if "R n >= 128
                            if (match == 0)
                                throw new ParseException("No match S block n>128: " + data, 0);

                            data = data.substring(strUpper.length()); //# skip sig

                            String ch_str = "";
                            //String ch_lotux;

                            for (int j = 0; j < 10; j++) { // # shouldn't be more than 10 hex chars

                                if (j > 1) { // # lotu check

                                    if (data.indexOf(strL) == 0) {
                                        data = data.substring(strL.length());
                                        //ch_lotux = "l";
                                        break;
                                    } else if (data.indexOf(strO) == 0) {
                                        data = data.substring(strO.length());
                                        //ch_lotux = "o";
                                        break;
                                    } else if (data.indexOf(strT) == 0) {
                                        data = data.substring(strT.length());
                                        //ch_lotux = "t";
                                        break;
                                    } else if (data.indexOf(strU) == 0) {
                                        data = data.substring(strU.length());
                                        //ch_lotux = "u";
                                        break;
                                    }
                                }

                                // # gv + "."+b[ c ]
                                if (data.indexOf(gvSig) == 0) {
                                    data = data.substring(gvSig.length());// # skip gvsig
                                    for (int k = 0; k < b.length; k++) //# for every entry in b
                                        if (data.indexOf(b[k]) == 0) {
                                            data = data.substring(b[k].length());
                                            ch_str += Integer.toHexString(k);
                                            break;
                                        }
                                } else
                                    break; // # done

                            }

                            out.append((char) (Integer.parseInt(ch_str, 16)));
                            //out.append(ch_lotux);
                            break; // # step out of the while loop

                        } else if (data.indexOf(strLower) == 0) { // r3 check if "R // n < 128
                            if (match == 0)
                                throw new ParseException("No match S block n<128: " + data, 0);

                            data = data.substring(strLower.length()); //# skip sig

                            String ch_str = "";
                            String ch_lotux = "";
                            String temp;
                            int b_checkR1 = 0;

                            for (int j = 0; j < 3; j++) { // # shouldn't be more than 3 octal chars
                                if (j > 1) { //# lotu check
                                    if (data.indexOf(strL) == 0) {
                                        data = data.substring(strL.length());
                                        ch_lotux = "l";
                                        break;
                                    } else if (data.indexOf(strO) == 0) {
                                        data = data.substring(strO.length());
                                        ch_lotux = "o";
                                        break;
                                    } else if (data.indexOf(strT) == 0) {
                                        data = data.substring(strT.length());
                                        ch_lotux = "t";
                                        break;
                                    } else if (data.indexOf(strU) == 0) {
                                        data = data.substring(strU.length());
                                        ch_lotux = "u";
                                        break;
                                    }
                                }

                                //# gv + "."+b[ c ]
                                if (data.indexOf(gvSig) == 0) {
                                    temp = data.substring(gvSig.length());
                                    for (int k = 0; k < 8; k++) // for every entry in b octal
                                        if (temp.indexOf(b[k]) == 0) {
                                            if (Integer.parseInt(ch_str + k, 8) > 128) {
                                                b_checkR1 = 1;
                                                break;
                                            }

                                            ch_str += k;
                                            data = data.substring(gvSig.length()); //#skip gvsig
                                            data = data.substring(b[k].length());
                                            break;
                                        }

                                    if (b_checkR1 == 1)
                                        if (data.indexOf(strHex) == 0) { //#0123456789 abcdef
                                            data = data.substring(strHex.length());
                                            //#check every element of hex decode string for a match
                                            for (int i = 0; i < b.length; i++)
                                                if (data.indexOf(b[i]) == 0) {
                                                    data = data.substring(b[i].length());
                                                    ch_lotux = Integer.toHexString(i);
                                                    break;
                                                }
                                        }
                                } else
                                    break;
                            }
                            out.append((char) Integer.parseInt(ch_str, 8));
                            out.append(ch_lotux);
                            break; // # step out of the while loop

                        } else if ((0x21 <= n && n <= 0x2f) ||
                                (0x3A <= n && n <= 0x40) ||
                                (0x5b <= n && n <= 0x60) ||
                                (0x7b <= n && n <= 0x7f)) {
                            out.append(data.charAt(0));
                            data = data.substring(1);
                            match += 1;
                        }
                    }

                    continue;
                }
            }

            throw new ParseException("No match: " + data, 0);
        }

        return out.toString();
    }

    private String clean(String str) {
        return str.replaceAll("^\\s+|\\s+$", "");
    }

    private ResultCheckPalindrome checkPalindrome(String Str) throws ParseException {
        return ResultCheckPalindrome.of(Str);
    }

    private static class ResultCheckPalindrome {
        final int startPos, endPos;
        final String gv;

        private ResultCheckPalindrome(int startPos, int endPos, String gv) {
            this.startPos = startPos;
            this.endPos = endPos;
            this.gv = gv;
        }

        static ResultCheckPalindrome of(String str) throws ParseException {
            int start, end;
            String gv;

            if (str.indexOf("\"'\\\"+'+\",") == 0) {
                start = str.indexOf("$$+\"\\\"\"+") + 8;
                gv = str.substring(str.indexOf("\"'\\\"+'+\",") + 9, str.indexOf("=~[]"));
            } else {
                start = str.indexOf("\"\\\"\"+") + 5;
                gv = str.substring(0, str.indexOf('='));
            }

            end = str.indexOf("\"\\\"\")())()");

            if (start == end) throw new ParseException("No data", end);

            return new ResultCheckPalindrome(start, end, gv);
        }
    }
}
