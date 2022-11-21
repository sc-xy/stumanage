package com.xystu;

import java.io.Serializable;

public class studentIdx implements Comparable<studentIdx>, Serializable {
    private char[] id = new char[7];
    private long offset = 0;

    public studentIdx() {
    }

    public studentIdx(String id, long offset) {
        setId(id);
        setOffset(offset);
    }

    public void setId(String id) {
        int tmp = id.length() > 7 ? 7 : id.length();
        for (int i = 0; i < tmp; i++)
            this.id[i] = id.charAt(i);
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public char[] getId() {
        return this.id;
    }

    public long getOffset() {
        return this.offset;
    }

    @Override
    public int compareTo(studentIdx other) {
        int num1 = Integer.parseInt(new String(this.id));
        int num2 = Integer.parseInt(new String(other.id));
        if (num1 == num2)
            return 0;
        if (num1 > num2)
            return 1;
        return -1;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof studentIdx) {
            for (int i = 0; i < 7; i++) {
                if (this.id[i] != ((studentIdx) other).id[i])
                    return false;
            }
            return true;
        }
        if (other instanceof String) {
            int i = 0;
            for (i = 0; i < ((String) other).length(); i++) {
                if (this.id[i] != ((String) other).charAt(i))
                    return false;
            }
            for (; i < 7; i++) {
                if (this.id[i] != '\000')
                    return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "id: " + new String(this.id) + "\noffset: " + this.offset + "\n";
    }

}
