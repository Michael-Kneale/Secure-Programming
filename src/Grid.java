import java.awt.Graphics;
import java.util.BitSet;
import java.util.Iterator;
import java.util.function.Consumer;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.io.FileInputStream;
import java.io.IOException;


public class Grid implements Iterable<Cell>{

    static {
        // must set before the Logger
        // loads logging.properties 
        try {
            //If the program cannot find the file, right-click on the .properties file, and get the relative path
            LogManager.getLogManager().readConfiguration(new FileInputStream("resources/logging.properties"));
        } catch (SecurityException | IOException e1) {
            //No logger, yet. Printing to console
            e1.printStackTrace();
        }
    }

    //import logger
    private static final Logger logger = Logger.getLogger(App.class.getName());

    Cell[][] cells;
    Counter counter;
    int activeRow;
    int activeColumn;
    int winCounter = 0;
    String wordToGuess;
    boolean gameFinished;
    SQLiteConnectionManager wordleDatabaseConnection;

    
    public Grid(int rows, int wordLength, SQLiteConnectionManager sqlConn){
        cells = new Cell[rows][wordLength];
        for (int i = 0; i < cells.length; i++) {
            for (int j = 0; j < cells[i].length; j++) {
                int x = 10 + 80 * i;
                int y = 10 + 80 * j;
                cells[i][j] = new Cell(i,j,y,x);
            }
        }
        activeRow = 0;
        activeColumn = 0;
        cells[activeRow][activeColumn].setActive();
        wordToGuess = "";
        gameFinished = false;
        wordleDatabaseConnection = sqlConn;
        counter = new Counter(570, 10);
    }

    void setWord(String word){
        wordToGuess = word;
    }

    public void paint(Graphics g) {
        doToEachCell((Cell c) -> c.paint(g));
        counter.paint(g);
    }

    public void reset(){
        cells[activeRow][activeColumn].setInactive();
        activeRow = 0;
        activeColumn = 0;
        gameFinished = false;
        doToEachCell((Cell c) -> c.reset());
        cells[activeRow][activeColumn].setActive();
        //how to get a new word?
    }

    /**
     * Takes a cell consumer (i.e. a function that has a single `Cell` argument and
     * returns `void`) and applies that consumer to each cell in the grid.
     *
     * @param func The `Cell` to `void` function to apply at each spot.
     */
    public void doToEachCell(Consumer<Cell> func) {
        for (int i = 0; i < cells.length; i++) {
            for (int j = 0; j < cells[i].length; j++) {
                func.accept(cells[i][j]);
            }
        }
    }

	@Override
	public Iterator<Cell> iterator() {
		return new CellIterator(cells);
	}

    void keyPressedBackspace(){
        if(!gameFinished){
            cells[activeRow][activeColumn].setCharacter(' ', 0);
            if(activeColumn > 0){
                cells[activeRow][activeColumn].setInactive();
                activeColumn--;
                cells[activeRow][activeColumn].setActive();
            }
        }
    }

    void keyPressedEscape(){
        reset();
    }

    void keyPressedEnter(){
        if(!gameFinished){
            
            //is the row full? If so, let's compare!
            if(activeColumn == cells[activeRow].length -1 && 
                !cells[activeRow][activeColumn].getStoredCharacter().equals(" ")){
                
                if(checkActiveRowAgainstWord()){
                    //success!
                    for(int i = 0; i < cells[activeRow].length; i++){
                        cells[activeRow][i].setInactive();
                        cells[activeRow][i].setState(3);
                    }
                    winCounter += 1;
                    counter.setCounter(String.valueOf(winCounter));
                    gameFinished = true;
                }else{
                    if(activeRow >= cells.length-1){
                        // run out of guesses to use
                        for(int i = 0; i < cells[activeRow].length; i++){
                            cells[activeRow][i].setInactive();
                            cells[activeRow][i].setState(4);
                        }
                        gameFinished = true;
                    }else{
                        //do stuff to highlihgt correct characters
                        applyHighlightingToCurrentRow();
                        //move to next row
                        cells[activeRow][activeColumn].setInactive();
                        activeRow++;
                        activeColumn = 0;
                        cells[activeRow][activeColumn].setActive();
                    }
                }
            }
        }
    }

    void keyPressedLetter(char letter){
        if(!gameFinished){
            logger.log(Level.INFO, "grid keypress received letter: " + letter);
            cells[activeRow][activeColumn].setCharacter(Character.toLowerCase(letter), 1);
            if(activeColumn < cells[activeRow].length -1){
                //not last character
                cells[activeRow][activeColumn].setInactive();
                activeColumn++;
                cells[activeRow][activeColumn].setActive();
            }
            // if they keep on typing characters, then just repopulate the last valye
        }
    }

    protected boolean checkActiveRowAgainstWord(){
        StringBuilder word =new StringBuilder("");

        for(int i = 0; i < cells[activeRow].length; i++){
            word.append(cells[activeRow][i].getStoredCharacter());
        }
        return word.toString().equals(wordToGuess);
    }

    protected void applyHighlightingToCurrentRow(){
        BitSet highlighted = new BitSet(cells[activeRow].length);
        highlighted.clear();

        //check for green characters
        for(int i = 0; i < cells[activeRow].length; i++){
            if(cells[activeRow][i].getStoredCharacter().equals(""+wordToGuess.charAt(i))){
                cells[activeRow][i].setState(3);
                highlighted.set(i);
            }
        }

        
        //crude method
        for(int i = 0; i < cells[activeRow].length; i++){
            if(!highlighted.get(i)){
                char c = cells[activeRow][i].getStoredCharacter().charAt(0);
                int countHighlighted = numberAlreadyHighlighted(c, cells[activeRow], highlighted);
                int countOccurrences = numberOccurrencesInGoal(c);

                if(wordToGuess.contains(cells[activeRow][i].getStoredCharacter())){
                    if(countHighlighted < countOccurrences){
                        cells[activeRow][i].setState(2); 
                        highlighted.set(i);
                    }
                }
            }
        }
    }

    protected int numberAlreadyHighlighted(char searchChar, Cell[] word, BitSet highlighted){
        // count the number of characters in the search word
        int counter = 0;
        for(int i = 0; i < word.length; i++){
            if(word[i].getStoredCharacter().equals(""+searchChar) && highlighted.get(i)){
                counter++;
            }
        }
        return counter;
    }

    protected int numberOccurrencesInGoal(char searchChar){
        int counter = 0;
        for( int i = 0; i < wordToGuess.length(); i++ ){
            if(wordToGuess.charAt(i) == searchChar){
                counter++;
            }
        }
        return counter;
    }

}
