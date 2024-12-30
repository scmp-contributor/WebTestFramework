package com.scmp.framework.utils;

public class StringUtils {

    /**
     * This function input a string with special characters for regex and returns the escaped string.
     * @param input the input string
     * @return the escaped string
     * */
    public static String getEscapedRegexString(String input){

        String returnInput = input;

        // List of special characters in regex
        String[] specialCharacters = { "^", "$", ".", "|", "?", "*", "+", "(", ")", "[", "]", "{", "}" };

        // Escape each special character
        for (String specialCharacter : specialCharacters) {
            returnInput = returnInput.replace(specialCharacter, "\\" + specialCharacter);
        }

        return returnInput;
    }
}
