package solver;

import java.util.*;
import java.lang.System;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

/**
 * Dictionary aware guessing strategy for Hangman. (task B)
 * You'll need to complete the implementation of this.
 *
 * @author Jeffrey Chan, RMIT 2020
 */

/**
 * References :
 * [1] paul, J.
 * paul, j. (2020) How to sort HashMap by values in Java 8 using Lambdas and Stream - Example Tutorial, Java67.com. Available at: https://www.java67.com/2017/07/how-to-sort-map-by-values-in-java-8.html (Accessed: 3 October 2020).
 */
public class DictAwareSolver extends HangmanSolver
{

    // Keeps the copy of dictory and then subsequently replaces with narrowed down subset
    private Set<String> knownWords = null;

    // Maintains the set of guessed chars that will be not be guessed again
    private List<Character> guessedChars = null;

    // Maintains the symbols and count of words that contains it
    private HashMap<Character, Integer> sampleSetCharFreqMap = null;

    // Same as above but sorted by count popularity in decreasing order
    HashMap<Character, Integer> sortedFreqMap  = null;

    private int wordLength = 0;

    /**
     * Constructor.
     *
     * @param dictionary Dictionary of words that the guessed words are drawn from.
     */
    public DictAwareSolver(Set<String> dictionary) {

        this.knownWords = dictionary;
        this.guessedChars = new ArrayList<Character>();
        this.sampleSetCharFreqMap = new HashMap<Character, Integer>();
        sortedFreqMap  = new HashMap<>();

    } // end of DictAwareSolver()


    @Override
    public void newGame(int[] wordLengths, int maxIncorrectGuesses)
    {
        // Added 17-10-2020 - wordLength was not being set
        wordLength = wordLengths[0];

        // Trim known words to contain words with word to be gussed length
        this.trimSampleSetBySize(wordLengths[0]);

        // Calculate the symbols and number of words with it
        this.calSampleSetCharFreqMap(this.knownWords);

    } // end of newGame()


    @Override
    public char makeGuess() {

        // Pick the most popular symbol
        //char probableGuess = (char) this.sortedFreqMap.keySet().toArray()[0];

        // Analysis shows that, if there is a tie, pick the latter, gives more accuracy. atleast for the given sampleset
        char probableGuess = sortedFreqMap.entrySet().stream()
                             .max((firstChar, secondChar) -> firstChar.getValue() > secondChar.getValue() ? 1 : -1)
                             .get()
                             .getKey();

        // Mark it as guessed - added - 17-10-2020
        this.guessedChars.add(probableGuess);

        return probableGuess;

    } // end of makeGuess()


    @Override
    public void guessFeedback(char c, Boolean bGuess, ArrayList< ArrayList<Integer> > lPositions)
    {

        // Mark it as guessed - again - So that during 2 or n word solver, sample size of other words can be reduced
        // For n-word solver
        if ( ! this.guessedChars.contains(c) )
            this.guessedChars.add(c);

        // Character Found, Reduce sampleset to words, with letter, at these positions
        if(bGuess){
            trimSampleSetByCharacter(c, "AT", lPositions.get(0));
        }else{
            // Wrong guess, Reduce sample set to words, not with this letters
            trimSampleSetByCharacter(c, "NOT_AT", lPositions.get(0));
        }

        // Recalculate the popularity of symbols in reduced dictionary
        this.calSampleSetCharFreqMap(this.knownWords);

    } // end of guessFeedback()


    /**
     * Resetting the frequency count of all alphasets in current sample set
     */
    public void resetSampleSetCharFreqMap(){

        // Run over a-z and set frequency count to 0
        this.sampleSetCharFreqMap = new HashMap<Character, Integer>() {
            {
                for (char alpha = 'a'; alpha <= 'z'; ++alpha)
                    put(Character.valueOf(alpha), 0);
            }
        };

        //System.out.println(sampleSetCharFreqMap);
    }

    /**
     * Calculates the frequency count of alphabets in current sample set
     * @param dictionary : current sample set
     */
    public void calSampleSetCharFreqMap(Set<String> dictionary){

        this.resetSampleSetCharFreqMap();

        // For every word in current sample set
        for(String currWord : dictionary){

            // Get all unique characters in a word
            String uniqueChars = Arrays.asList(currWord.split("")).stream().distinct().collect(Collectors.joining());

            // Iterate over those chars and increament occurance count
            for(char currAlpha : uniqueChars.toCharArray()){
                try{
                    sampleSetCharFreqMap.compute(currAlpha, (alpha, freq) -> freq+1);
                }catch (NullPointerException np){

                    // Changed from 0 to 1 to see the effect, special character like ' was not being picked up
                    sampleSetCharFreqMap.compute(currAlpha, (alpha, freq) -> freq == null ? 1 : freq + 1);
                }
            }
        }

        // [1] - Sorting map based on occurance count.
        this.sortedFreqMap = this.sampleSetCharFreqMap.entrySet().stream()
                                                                .sorted(Map.Entry.<Character, Integer> comparingByValue().reversed() )
                                                                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        // Removing guessed characters from frequency set
        for(char guessed : this.guessedChars){
            this.sortedFreqMap.remove(guessed);
        }

        //System.out.println(sortedFreqMap);
    }


    public void recalSampleSet(){

    }


    /**
     * Filters the list of words, that are of length of word to be guessed.
     * Hopefully this will reduce the initial sample size
     * @param wordSize
     */
    public void trimSampleSetBySize(int wordSize){

        this.knownWords = this.knownWords.stream().filter(word -> word.length() == wordSize).collect(Collectors.toSet());
    }


    /**
     * Reduces the knownWords set
     * @param c - guessed character
     * @param op - AT or NOT_AT - specificies the filter condition
     * @param lPositions - positions where c was found, if found
     * @return
     */
    public void trimSampleSetByCharacter(char c, String op, ArrayList<Integer> lPositions){

        Set<String> reducedSet = new HashSet<>();

        switch (op){
            case "AT" :
            {
                // For every word
                for(String word : this.knownWords){
                    boolean matched = true;

                    // For every found positions
                    for(int pos : lPositions){

                        // If at any position, char is not there in word, that word will not be picked up
                        if(word.charAt(pos) != c){
                            matched = false;
                            break;
                        }
                    }

                    // Word contains char c at all specified positions
                    if(matched){
                        reducedSet.add(word);
                    }
                }
            }
            break;

            // Removes the words that contains letter in it, irrespective of position
            case "NOT_AT" :
            {
                reducedSet = this.knownWords.stream().filter(word -> !word.contains(String.valueOf(c))).collect(Collectors.toSet());
            }
            break;
        }

        this.knownWords = reducedSet;
    }



    // Accessor methods starts here

    public Set<String> getKnownWords() {
        return knownWords;
    }

    public void setKnownWords(Set<String> knownWords) {
        this.knownWords = knownWords;
    }

    public List<Character> getGuessedChars() {
        return guessedChars;
    }

    public void setGuessedChars(List<Character> guessedChars) {
        this.guessedChars = guessedChars;
    }

    public HashMap<Character, Integer> getSampleSetCharFreqMap() {
        return sampleSetCharFreqMap;
    }

    public void setSampleSetCharFreqMap(HashMap<Character, Integer> sampleSetCharFreqMap) {
        this.sampleSetCharFreqMap = sampleSetCharFreqMap;
    }

    public HashMap<Character, Integer> getSortedFreqMap() {
        return sortedFreqMap;
    }

    public void setSortedFreqMap(LinkedHashMap<Character, Integer> sortedFreqMap) {
        this.sortedFreqMap = sortedFreqMap;
    }

    public int getWordLength() {
        return wordLength;
    }

    public void setWordLength(int wordLength) {
        this.wordLength = wordLength;
    }
    // Accessor methods ends here


} // end of class DictAwareSolver
