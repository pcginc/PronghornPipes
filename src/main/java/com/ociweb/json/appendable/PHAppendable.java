package com.ociweb.json.appendable;

public interface PHAppendable extends Appendable {
    Appendable append(CharSequence csq);

    Appendable append(CharSequence csq, int start, int end);

    Appendable append(char c);
}
