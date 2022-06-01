//connectivity libraries
import oscP5.*;
import netP5.*;

// utils libraries
import java.util.Random;
import java.util.Arrays;

// libraries for osc handling
import java.sql.Blob;
import javax.sql.rowset.serial.SerialBlob;
import java.nio.*;
import java.nio.IntBuffer;
import java.nio.ByteBuffer;

// OSC
OscP5 oscP5;
NetAddress remoteLocation;
static final int portNumber = 7777;
static final String address = "127.0.0.1";
static final int intByteLength = 4;
OscHandler oscHandler = new OscHandler();

// KEYBOARD
final static int keysNumber = 12;
final static int numOctaves = 3;
boolean[] pressedKeysFlagArray = new boolean[36];
RootNameDisplayer rootNameDisplayer = new RootNameDisplayer();
PianoKeyboardDisplayer pianoKeyboardDisplayer = new PianoKeyboardDisplayer();

// WAVES - SUN
// TODO: WHY height width don't work well?
final static int canvasWidth = 1920;
final static int canvasHeight = 1080;
final static float reactFactor = min(canvasWidth/3000.0, canvasHeight/2000.0);
final static int yHorizonHeight = floor(canvasHeight/3);
final static int waveNum = 11;
final static int waveDistanceIncrement = round(40*reactFactor);
Wave[] waves = new Wave[waveNum];
SunHandler sunHandler;

//COLORS
color skyColor = color(245, 87, 66);
color sunCenterColor = color(255, 227, 115);
color sunOuterColor = color(252, 156, 84);
float wavePaletteStartInd = 0;

// OTHER SHARED OBJECTS (HELPERS AND HANDLERS)
PerspectiveHelper perspectiveHelper = new PerspectiveHelper();
ChordBasedColorPicker chordBasedColorPicker = new ChordBasedColorPicker();
ColorIntepolator colorIntepolator = new ColorIntepolator();
ChordRecogniser chordRecogniser = new ChordRecogniser();
Random random = new Random();
int CURRENT_NOTE_ID = 0;
int[] CURRENT_HARMONIZATION = {0, 0, 0, 0};

// TODO: DELETE - TEMP FOR TESTING
String[] typesArray = {"maj", "sus", "mindom7", "min", "majdom7", "majmaj7"};
color[] tempColors = new color[waveNum];

void setup() {
  
  //fullScreen(); // Fullscreen Setup
  size(1920, 1080); // Full HD setup 
  background(skyColor);
  int divideLineY = ceil(height*3/4);
  
  // OSCP5 setup
  oscP5 = new OscP5(this, portNumber);
  remoteLocation = new NetAddress(address, portNumber);

  // setup Sun
  sunHandler = new SunHandler(sunCenterColor, sunOuterColor, canvasHeight/4);
  
  // setup Waves
  float distCoeff;
  float distCoeffSqr;
  float distCoeffCube;
  float waveDistance = waveDistanceIncrement;
  for(int i=0; i<waveNum; i++) {
    distCoeff = perspectiveHelper.distFactor(yHorizonHeight + canvasHeight*i/10);
    distCoeffSqr = distCoeff*distCoeff;
    distCoeffCube = distCoeffSqr*distCoeff;
    waveDistance = waveDistance + waveDistanceIncrement;
    tempColors[i] = chordBasedColorPicker.getPalette()[i%chordBasedColorPicker.getPalette().length];
    waves[i] = new Wave(yHorizonHeight + round(waveDistance*distCoeffSqr), distCoeffCube*3000.0, random(PI, 2*PI), 100.0*(distCoeffSqr), tempColors[i], random(-0.15, 0.15));
  }
  // Display letters and symbols representing the currently playing chord - scaling numbers are just for alignment
  rootNameDisplayer.create(width/4, height*59/64);
  
  // Create the keyboard
  pianoKeyboardDisplayer.setX(width/3);
  pianoKeyboardDisplayer.setY(divideLineY + (height-divideLineY)/4);
  pianoKeyboardDisplayer.drawKeyboard(numOctaves, pressedKeysFlagArray);
}

void draw() {
  background(skyColor);
  
  // Render Sun
  sunHandler.drawSun(canvasWidth/2, yHorizonHeight + (yHorizonHeight/15));
  
  // Render waves
  wavePaletteStartInd += 0.1;
  for(int i=0; i<waveNum; i++) {
    if(round(wavePaletteStartInd*10)%10 == 0) {
      waves[i].scaleAmplitude(random(0.97, 1.03));
  }
    tempColors[i] = colorIntepolator.interpolate(tempColors[i], chordBasedColorPicker.palette[(abs(floor(-wavePaletteStartInd)+i))%chordBasedColorPicker.palette.length], 0.25);
    //tempColors[i] = colorIntepolator.interpolate(tempColors[(abs(floor(-wavePaletteStartInd)+i))%palette.length], palette[(abs(floor(-wavePaletteStartInd)+i))%palette.length], 0.02);
    // old code: waves[i].setColor(palette[(abs(floor(-wavePaletteStartInd)+i))%palette.length]);
    waves[i].setColor(tempColors[i]);
    //waves[i].setColor(tempColors[i]);

    waves[i].calcWave();
    waves[i].renderWave();
  }
  
  // chord displayer and keyboard
  rootNameDisplayer.updateRoot();
  pianoKeyboardDisplayer.drawKeyboard(numOctaves, pressedKeysFlagArray);
}

// wants a message built like this:
// [0] - Boolean indicating if its note on or note off
// [1] - Root freq midi number
// [2] - Blob containg the freq midi number of the harmonized voices
void oscEvent(OscMessage inOscMessage) {
  print("OscEvent: ");
  print(inOscMessage.typetag() + " - ");

  if(oscHandler.isOscMessageNoteOn(inOscMessage)){
     print("NoteOn - root: ");
     // get root midi number
     int root = oscHandler.getRoot(inOscMessage);
     print(root + " - ");
     int[] armonizedVoicesMidiNotes = oscHandler.getHarmonizedNotes(inOscMessage);
     print("armonized voices: ");
     for(int i=0; i<armonizedVoicesMidiNotes.length; i++){
       print(armonizedVoicesMidiNotes[i] + " ");
     }
       println();
     if(CURRENT_NOTE_ID != root || !Arrays.equals(CURRENT_HARMONIZATION, armonizedVoicesMidiNotes)){ 
       println("Note and harmonization have changed");
       println("CURRENT_HARMONIZATION: - length: " + CURRENT_HARMONIZATION.length + " voices: " + CURRENT_HARMONIZATION[0] + " - " + CURRENT_HARMONIZATION[1] + " - " + CURRENT_HARMONIZATION[2] + " - " + CURRENT_HARMONIZATION[3]);
       println("armonizedVoicesMidiNotes: - length: " + armonizedVoicesMidiNotes.length + " voices: " + armonizedVoicesMidiNotes[0] + " - " + armonizedVoicesMidiNotes[1] + " - " + armonizedVoicesMidiNotes[2] + " - " + armonizedVoicesMidiNotes[3]);

       CURRENT_NOTE_ID = root;
       CURRENT_HARMONIZATION = armonizedVoicesMidiNotes;
       noteOnOSC(root, armonizedVoicesMidiNotes);
     } else {
       println("Note and harmonization not changed");
     }

  } else {
    noteOffOSC();
    println("NoteOff");
  }
  println("\n");
}

// OSC NoteOn - notes working with midi freq values
void noteOnOSC(int root, int[] armonizedVoicesMidiNotes) {
  println("Note on: " + root);
  
  // Note displayer response
  rootNameDisplayer.setRootName(chordRecogniser.midiFreq2Note(root));  
  // Keyboard response 
  Arrays.fill(pressedKeysFlagArray, false);
  pressedKeysFlagArray[root%(12*numOctaves)] = true;
  for(int i=0; i<armonizedVoicesMidiNotes.length; i++){
      pressedKeysFlagArray[armonizedVoicesMidiNotes[i]%(12*numOctaves)] = true;
  }
  // Sun response
  sunHandler.setPulsing(true);
  // Waves Response
  String chordType = chordRecogniser.recogniseChordType(root, armonizedVoicesMidiNotes);
  println("Chord type: " + chordType);
  chordBasedColorPicker.generatePalette(chordType); // set color palette
  for(int i=0; i<waveNum; i++){
    waves[i].scaleThetaIncrement(random.nextBoolean() ? random(0.8, 1.2) : -random(0.8, 1.2)); // wave speed
    waves[i].scaleAmplitude(random(0.97, 1.03)); // amplitude
  }
 }

// OSC NoteOff 
void noteOffOSC() {
  sunHandler.setPulsing(false);
  Arrays.fill(pressedKeysFlagArray, false);

}

public class RootNameDisplayer {
  private String chordName = ""; 
  private color nameColor = color(252, 186, 3);
  private color circleColor = color(15, 0, 46);
  private color circleFrameColor = color(252, 186, 3);
  private int frameRadius = round(276*reactFactor);
  private int textSize = round(192*reactFactor);
  private int x;
  private int y;

  public String getRootName() {
    return this.chordName;
  }

  public void setRootName(String chordName) {
    this.chordName = chordName;
  }
  
  public void updateRoot(){
    fill(this.circleColor);
    rectMode(CORNER);
    stroke(this.circleFrameColor);
    strokeWeight(24);  
    stroke(this.nameColor); 
    circle(this.x+this.frameRadius/80, this.y - this.frameRadius/4, this.frameRadius);
    textAlign(CENTER);
    fill(this.nameColor);
    textSize(this.textSize);
    text(this.chordName, this.x, this.y); 
  }
  
  public void create(int x, int y){
    this.x = x;
    this.y = y;
    fill(255);
    rectMode(CORNER);
    strokeWeight(24);  
    stroke(this.nameColor); 
    circle(x+this.frameRadius/80, y - this.frameRadius/4, this.frameRadius);
    textAlign(CENTER);
    fill(this.nameColor);
    textSize(this.textSize);
    text(this.chordName, x, y); 
  }
}

public class PianoKeyboardDisplayer {
  private int x = width/2;
  private color y = height/2;
  private int whiteKeyBottomWidth  = round(90*reactFactor);
  private int whiteKeyHeight  = round(300*reactFactor);
  private int blackKeyWidth = round(64*reactFactor);
  private int blackKeyHeight = round(200*reactFactor);
  private int whiteKeyTopWidth = round((this.whiteKeyBottomWidth - reactFactor*this.blackKeyWidth*2/3));
  private color keysColor = color(252, 186, 3);
  private color pressedKeysColor = color(99, 99, 99);
  private boolean[] activeNotesFlagArray = new boolean[keysNumber];
  
  public void createCShapedkey(int startX, int startY, boolean pressed){
     strokeWeight(0);
     stroke(keysColor);
     if(pressed){ fill(this.pressedKeysColor); } else { fill(this.keysColor); }
     // Upper part of the Key
     rect(startX, startY, this.whiteKeyTopWidth, this.blackKeyHeight);
     // Lower part of the Key
     rect(startX, startY+this.blackKeyHeight, this.whiteKeyBottomWidth, this.whiteKeyHeight-this.blackKeyHeight);
     // Manually draw Outline
     stroke(0);
     strokeWeight(1); 
     beginShape(LINES);
     vertex(startX, startY);
     vertex(startX+this.whiteKeyTopWidth, startY);
     
     vertex(startX+this.whiteKeyTopWidth, startY);
     vertex(startX+this.whiteKeyTopWidth, startY+this.blackKeyHeight);
     
     vertex(startX+this.whiteKeyTopWidth, startY+this.blackKeyHeight);
     vertex(startX+this.whiteKeyBottomWidth, startY+this.blackKeyHeight);

     vertex(startX+this.whiteKeyBottomWidth, startY+this.blackKeyHeight);
     vertex(startX+this.whiteKeyBottomWidth, startY+this.whiteKeyHeight);
     
     vertex(startX+this.whiteKeyBottomWidth, startY+this.whiteKeyHeight);
     vertex(startX, startY+this.whiteKeyHeight);
     
     vertex(startX, startY+this.whiteKeyHeight);
     vertex(startX, startY);

     endShape();
     strokeWeight(0); 
  }
  
  public void createDShapedkey(int startX, int startY, boolean pressed){
     strokeWeight(0); 
     stroke(keysColor);
     if(pressed){ fill(this.pressedKeysColor); } else { fill(this.keysColor); }
     // Upper part of the Key
     rect(startX+this.blackKeyWidth/3, startY, this.whiteKeyTopWidth, this.blackKeyHeight);
     // Lower part of the Key
     rect(startX, startY+this.blackKeyHeight, this.whiteKeyBottomWidth, this.whiteKeyHeight-this.blackKeyHeight);
     fill(0, 0);
     // Manually draw Outline
     stroke(0);
     strokeWeight(1); 
     beginShape(LINES);
     vertex(startX+this.blackKeyWidth/3, startY);
     vertex(startX+this.blackKeyWidth/3+this.whiteKeyTopWidth, startY);
     
     vertex(startX+this.blackKeyWidth/3+this.whiteKeyTopWidth, startY);
     vertex(startX+this.blackKeyWidth/3+this.whiteKeyTopWidth, startY+this.blackKeyHeight);
     
     vertex(startX+this.blackKeyWidth/3+this.whiteKeyTopWidth, startY+this.blackKeyHeight);
     vertex(startX+this.whiteKeyBottomWidth, startY+this.blackKeyHeight);

     vertex(startX+this.whiteKeyBottomWidth, startY+this.blackKeyHeight);
     vertex(startX+this.whiteKeyBottomWidth, startY+this.whiteKeyHeight);
     
     vertex(startX+this.whiteKeyBottomWidth, startY+this.whiteKeyHeight);
     vertex(startX, startY+this.whiteKeyHeight);
     
     vertex(startX, startY+this.whiteKeyHeight);
     vertex(startX, startY+this.blackKeyHeight);
     
     vertex(startX, startY+this.blackKeyHeight);
     vertex(startX+this.blackKeyWidth/3, startY+this.blackKeyHeight);

     vertex(startX+this.blackKeyWidth/3, startY+this.blackKeyHeight);
     vertex(startX+this.blackKeyWidth/3, startY);

     endShape();
     strokeWeight(0); 
  }
  
  public void createEShapedkey(int startX, int startY, boolean pressed){
     strokeWeight(0); 
     stroke(keysColor);
     if(pressed){ fill(this.pressedKeysColor); } else { fill(this.keysColor); }
     // Upper part of the Key
     rect(startX+this.blackKeyWidth*2/3, startY, this.whiteKeyTopWidth, this.blackKeyHeight);
     // Lower part of the Key
     rect(startX, startY+this.blackKeyHeight, this.whiteKeyBottomWidth, this.whiteKeyHeight-this.blackKeyHeight);
     fill(0, 0);

     // Manually draw Outline
     stroke(0);
     strokeWeight(1); 
     beginShape(LINES);
     vertex(startX+this.blackKeyWidth*2/3, startY);
     vertex(startX+this.whiteKeyBottomWidth, startY);
     
     vertex(startX+this.whiteKeyBottomWidth, startY);
     vertex(startX+this.whiteKeyBottomWidth, startY+this.whiteKeyHeight);
     
     vertex(startX+this.whiteKeyBottomWidth, startY+this.whiteKeyHeight);
     vertex(startX, startY+this.whiteKeyHeight);

     vertex(startX, startY+this.whiteKeyHeight);
     vertex(startX, startY+this.blackKeyHeight);
     
     vertex(startX, startY+this.blackKeyHeight);
     vertex(startX+this.blackKeyWidth*2/3, startY+this.blackKeyHeight);
     
     vertex(startX+this.blackKeyWidth*2/3, startY+this.blackKeyHeight);
     vertex(startX+this.blackKeyWidth*2/3, startY);

     endShape();
     strokeWeight(0); 
  }
  
  public void createFShapedkey(int startX, int startY, boolean pressed){
     strokeWeight(0);
     stroke(keysColor);
     if(pressed){ fill(this.pressedKeysColor); } else { fill(this.keysColor); }
     // Upper part of the Key
     rect(startX, startY, this.whiteKeyTopWidth, this.blackKeyHeight);
     // Lower part of the Key
     rect(startX, startY+this.blackKeyHeight, this.whiteKeyBottomWidth, this.whiteKeyHeight-this.blackKeyHeight);
     fill(0, 0);

     // Manually draw Outline
     stroke(0);
     strokeWeight(1); 
     beginShape(LINES);
     vertex(startX, startY);
     vertex(startX+this.whiteKeyTopWidth, startY);
     
     vertex(startX+this.whiteKeyTopWidth, startY);
     vertex(startX+this.whiteKeyTopWidth, startY+this.blackKeyHeight);
     
     vertex(startX+this.whiteKeyTopWidth, startY+this.blackKeyHeight);
     vertex(startX+this.whiteKeyBottomWidth, startY+this.blackKeyHeight);

     vertex(startX+this.whiteKeyBottomWidth, startY+this.blackKeyHeight);
     vertex(startX+this.whiteKeyBottomWidth, startY+this.whiteKeyHeight);
     
     vertex(startX+this.whiteKeyBottomWidth, startY+this.whiteKeyHeight);
     vertex(startX, startY+this.whiteKeyHeight);
     
     vertex(startX, startY+this.whiteKeyHeight);
     vertex(startX, startY);

     endShape();
     strokeWeight(0); 
  }
  
   public void createGShapedkey(int startX, int startY, boolean pressed){
     strokeWeight(0); 
     stroke(keysColor);
     if(pressed){ fill(this.pressedKeysColor); } else { fill(this.keysColor); }
     // Upper part of the Key
     rect(startX+this.blackKeyWidth/3, startY, this.whiteKeyBottomWidth-this.blackKeyWidth*5/6, this.blackKeyHeight);
     // Lower part of the Key
     rect(startX, startY+this.blackKeyHeight, this.whiteKeyBottomWidth, this.whiteKeyHeight-this.blackKeyHeight);
     fill(0, 0);
     
     // Manually draw Outline
     stroke(0);
     strokeWeight(1); 
     beginShape(LINES);
     vertex(startX+this.blackKeyWidth/3, startY);
     vertex(startX+this.blackKeyWidth/3+this.whiteKeyBottomWidth-this.blackKeyWidth*5/6, startY);
     
     vertex(startX+this.blackKeyWidth/3+this.whiteKeyBottomWidth-this.blackKeyWidth*5/6, startY);
     vertex(startX+this.blackKeyWidth/3+this.whiteKeyBottomWidth-this.blackKeyWidth*5/6, startY+this.blackKeyHeight);
     
     vertex(startX+this.blackKeyWidth/3+this.whiteKeyBottomWidth-this.blackKeyWidth*5/6, startY+this.blackKeyHeight);
     vertex(startX+this.whiteKeyBottomWidth, startY+this.blackKeyHeight);

     vertex(startX+this.whiteKeyBottomWidth, startY+this.blackKeyHeight);
     vertex(startX+this.whiteKeyBottomWidth, startY+this.whiteKeyHeight);
     
     vertex(startX+this.whiteKeyBottomWidth, startY+this.whiteKeyHeight);
     vertex(startX, startY+this.whiteKeyHeight);
     
     vertex(startX, startY+this.whiteKeyHeight);
     vertex(startX, startY+this.blackKeyHeight);
     
     vertex(startX, startY+this.blackKeyHeight);
     vertex(startX+this.blackKeyWidth/3, startY+this.blackKeyHeight);

     vertex(startX+this.blackKeyWidth/3, startY+this.blackKeyHeight);
     vertex(startX+this.blackKeyWidth/3, startY);
     
     endShape();
     strokeWeight(0); 
  }
  
   public void createAShapedkey(int startX, int startY, boolean pressed){
      strokeWeight(0); 
      stroke(keysColor);
     if(pressed){ fill(this.pressedKeysColor); } else { fill(this.keysColor); }
     // Upper part of the Key
     rect(startX+this.blackKeyWidth/2, startY, this.whiteKeyBottomWidth-this.blackKeyWidth*5/6, this.blackKeyHeight);
     // Lower part of the Key
     rect(startX, startY+this.blackKeyHeight, this.whiteKeyBottomWidth, this.whiteKeyHeight-this.blackKeyHeight);
     fill(0, 0);
     
     // Manually draw Outline
     stroke(0);
     strokeWeight(1); 
     beginShape(LINES);
     vertex(startX+this.blackKeyWidth/2, startY);
     vertex(startX+this.blackKeyWidth/2+this.whiteKeyBottomWidth-this.blackKeyWidth*5/6, startY);
     
     vertex(startX+this.blackKeyWidth/2+this.whiteKeyBottomWidth-this.blackKeyWidth*5/6, startY);
     vertex(startX+this.blackKeyWidth/2+this.whiteKeyBottomWidth-this.blackKeyWidth*5/6, startY+this.blackKeyHeight);
     
     vertex(startX+this.blackKeyWidth/2+this.whiteKeyBottomWidth-this.blackKeyWidth*5/6, startY+this.blackKeyHeight);
     vertex(startX+this.whiteKeyBottomWidth, startY+this.blackKeyHeight);

     vertex(startX+this.whiteKeyBottomWidth, startY+this.blackKeyHeight);
     vertex(startX+this.whiteKeyBottomWidth, startY+this.whiteKeyHeight);
     
     vertex(startX+this.whiteKeyBottomWidth, startY+this.whiteKeyHeight);
     vertex(startX, startY+this.whiteKeyHeight);
     
     vertex(startX, startY+this.whiteKeyHeight);
     vertex(startX, startY+this.blackKeyHeight);
     
     vertex(startX, startY+this.blackKeyHeight);
     vertex(startX+this.blackKeyWidth/2, startY+this.blackKeyHeight);

     vertex(startX+this.blackKeyWidth/2, startY+this.blackKeyHeight);
     vertex(startX+this.blackKeyWidth/2, startY);
     
     endShape();
     strokeWeight(0); 
  }
  
   public void createBShapedkey(int startX, int startY, boolean pressed){
     strokeWeight(0); 
     stroke(keysColor);
     if(pressed){ fill(this.pressedKeysColor); } else { fill(this.keysColor); }
     // Upper part of the Key
     rect(startX+this.blackKeyWidth*2/3, startY, this.whiteKeyBottomWidth-this.blackKeyWidth*2/3, this.blackKeyHeight);
     // Lower part of the Key
     rect(startX, startY+this.blackKeyHeight, this.whiteKeyBottomWidth, this.whiteKeyHeight-this.blackKeyHeight);
     fill(0, 0);
     
     // Manually draw Outline
     stroke(0);
     strokeWeight(1); 
     beginShape(LINES);
     vertex(startX+this.blackKeyWidth*2/3, startY);
     vertex(startX+this.whiteKeyBottomWidth, startY);
     
     vertex(startX+this.whiteKeyBottomWidth, startY);
     vertex(startX+this.whiteKeyBottomWidth, startY+this.whiteKeyHeight);
     
     vertex(startX+this.whiteKeyBottomWidth, startY+this.whiteKeyHeight);
     vertex(startX, startY+this.whiteKeyHeight);

     vertex(startX, startY+this.whiteKeyHeight);
     vertex(startX, startY+this.blackKeyHeight);
     
     vertex(startX, startY+this.blackKeyHeight);
     vertex(startX+this.blackKeyWidth*2/3, startY+this.blackKeyHeight);
     
     vertex(startX+this.blackKeyWidth*2/3, startY+this.blackKeyHeight);
     vertex(startX+this.blackKeyWidth*2/3, startY);

     endShape();
     strokeWeight(0); 
  }
  
  private void createBlackKey(int startX, boolean pressed){
     // Upper part of the Key
     strokeWeight(0); 
     stroke(0);
     
     if(pressed){ 
       fill(this.pressedKeysColor);
       strokeWeight(0); 
     } else {
       fill(0);
       stroke(0);
     }

     rect(startX, this.y, this.blackKeyWidth, this.blackKeyHeight);
     // No Lower part of the Key
     fill(this.keysColor);
     stroke(0);
     strokeWeight(0); 
  }
  
  private void createOctave(int startX, boolean[] newKeyFlags){
    stroke(0);
    strokeWeight(0); 
    createCShapedkey(startX, this.y, newKeyFlags[0]);
    createBlackKey(startX + this.whiteKeyBottomWidth - this.blackKeyWidth*2/3, newKeyFlags[1]);
    createDShapedkey(startX + this.whiteKeyBottomWidth, this.y, newKeyFlags[2]);
    createBlackKey(startX + this.whiteKeyBottomWidth*2 - this.blackKeyWidth*1/3, newKeyFlags[3]);
    createEShapedkey(startX + this.whiteKeyBottomWidth*2, this.y, newKeyFlags[4]);
    createFShapedkey(startX + this.whiteKeyBottomWidth*3, this.y, newKeyFlags[5]);
    createBlackKey(startX + this.whiteKeyBottomWidth*4 - this.blackKeyWidth*2/3, newKeyFlags[6]);
    createGShapedkey(startX + this.whiteKeyBottomWidth*4, this.y, newKeyFlags[7]);
    createBlackKey(startX + this.whiteKeyBottomWidth*5 - this.blackKeyWidth/2, newKeyFlags[8]);
    createAShapedkey(startX + this.whiteKeyBottomWidth*5, this.y, newKeyFlags[9]);
    createBlackKey(startX + this.whiteKeyBottomWidth*6 - this.blackKeyWidth/3, newKeyFlags[10]);
    createBShapedkey(startX + this.whiteKeyBottomWidth*6, this.y, newKeyFlags[11]);
  }
  
  public void drawKeyboard(int numOctaves, boolean[] newKeyFlags){
    rectMode(CORNER);
    strokeWeight(16);  
    stroke(this.keysColor); 
    boolean[] oneOctaveFlags = new boolean[12];

    for (int i=0; i<numOctaves; i++){
      //todo shift octave starting point each iteration
      System.arraycopy(newKeyFlags, i*12, oneOctaveFlags, 0, 12);
      createOctave(this.x + i * this.whiteKeyBottomWidth * 7, oneOctaveFlags);
    }
  }
    
  public int getX() {
    return this.x;
  }
  public void setX(int x) {
    this.x = x;
  }
    public int getY() {
    return this.y;
  }
  public void setY(int y) {
    this.y = y;
  }
  public void setKeysColor(color keysColor){
    this.keysColor = keysColor;
  }
}

public class PerspectiveHelper {
  float xVanishPt = canvasWidth/2;
  float yVanishPt = canvasHeight/3;;
  
  float distFactor(int y) {
    float vanishPtDist = canvasHeight - this.yVanishPt;
    float distFromVanishPt = y - this.yVanishPt;
    return 0.01 + (distFromVanishPt/vanishPtDist);
  }
}

public class Wave{
  private int yPos = canvasHeight/2;
  private int xspacing = 8;                          // How far apart should each horizontal location be spaced
  private int waveWidth = canvasWidth+16;            // Width of entire wave
  private float theta = 0.0;                         // Start angle at 0
  private float amplitude;                           // Height of wave
  private float period;                              // How many pixels before the wave repeats
  private float dx;                                  // Value for incrementing X, a function of period and xspacing
  private float[] yvalues = new float[waveWidth/xspacing];  // Using an array to store height values for the wave
  private float thetaIncrement;
  private color waveColor;
  
  Wave(){}
  Wave(int yPos, float period, float theta, float amplitude, color waveColor, float thetaIncrement){
    this.period = period*reactFactor;
    this.yPos = yPos;
    this.theta = theta;
    this.amplitude = amplitude*reactFactor;
    this.dx = (TWO_PI / period) * xspacing;   
    this.waveColor = waveColor;
    this.thetaIncrement = thetaIncrement;
  }
  
  public void setColor(color waveColor){
    this.waveColor = waveColor;
  }
   
  public void scaleThetaIncrement(float scalingFactor){
    // Waht really contorls this is dx, but it's not so readable, so let's call it period even if not true
    this.thetaIncrement = this.thetaIncrement*scalingFactor;
    // compensate for the caused phase shift
  }
  
  public void scaleAmplitude(float scalingFactor){
    this.amplitude = this.amplitude*scalingFactor;
  }
  
  public void shiftPhase(float phaseShift){
    this.theta = this.theta + phaseShift;
  }
 
  public void calcWave() {
    // Increment theta (try different values for 'angular velocity' here
    this.theta += this.thetaIncrement;
    // For every x value, calculate a y value with sine function
    float x = this.theta;
    for (int i = 0; i < yvalues.length; i++) {
      this.yvalues[i] = sin(x)*this.amplitude;
      x+=this.dx;
    }
  }

  public void renderWave() {
    //TODO: nostroke??
    strokeWeight(4);
    stroke(255);
    fill(this.waveColor);
    // create sinusoidal curve and close it in order to be able to fill it
    beginShape();
    for(int i=0; i<this.yvalues.length; i++){
        curveVertex(i*this.xspacing, this.yPos+this.yvalues[i]);
    }  
    vertex(width, this.yPos+this.yvalues[this.yvalues.length-1]);
    vertex(width, height-1);
    vertex(width, height-1);
    vertex(0, height-1);
    vertex(0, height-1);
    vertex(0, this.yPos+this.yvalues[0]);
    endShape();
  }
}

public class SunHandler{
  private color sunColor;
  private color sunStrokeColor;
  private int radius;
  private SinScaler sinScale = new SinScaler();
  private boolean isPulsing = false;

  SunHandler(color sunColor, color sunStrokeColor, int radius){
    this.sunColor = sunColor;
    this.sunStrokeColor = sunStrokeColor;
    this.radius = radius;
  }
  
  public void setRadius(int radius){
    this.radius = radius;
  }
  
  public void drawSun(int x, int y){
    this.processSun();
    stroke(this.sunStrokeColor);
    strokeWeight(20);
    fill(this.sunColor);
    circle(x, y, this.radius*(1+sinScale.getPosScaling()/2));
  }
  
  public void processSun(){
    if(this.isPulsing){
      this.sinScale.calcScaling();
    }
  }
  
  public void setPulsing(boolean isPulsing){
    this.isPulsing = isPulsing;
  }
}

public class SinScaler {
  private float currAmplitude;
  private float theta = 0.0;         
  private float thetaIncrement = 0.1;
  
  public void calcScaling(){
    // Theta increment
    this.theta += this.thetaIncrement;
    currAmplitude = sin(this.theta);
  }
  
  public float getPosScaling(){
    return abs(this.currAmplitude);
  }
  
  public float getScaling(){
    return this.currAmplitude;
  }
  
  public void setIncrement(float increment){
    this.thetaIncrement = increment;
  }
}

// To use it correctly, remember to update the palette each time a chord is changed
class ChordBasedColorPicker {
  private int chordFirst = 0; // C=0, B=11
  private String chordType = "maj";
  private color generatedColor = color(0);
  private int paletteLength = 12;
  color[] palette = {
    color(8, 24, 58),
    color(21, 40, 82),
    color(75, 61, 96),
    color(253, 94, 83),
    color(239, 84, 17),
    color(252, 156, 84),
    color(255, 227, 115),
    color(254, 76, 1),
    color(142, 199, 210),
    color(13, 105, 134),
    color(0, 150, 136),
    color(4, 117, 111)
  };
  
  public void setChordFirst(int chordFirst) {
    this.chordFirst = chordFirst;
  }

  public void setChordType(String chordType) {
    this.chordType = chordType;
  }

  public int getChordFirst() {
    return this.chordFirst;
  }

  public String getChordType() {
    return this.chordType;
  }
  
  public color getGeneratedColor() {
    return this.generatedColor;
  }  
    
  public color[] getPalette(){
      return this.palette;
  }
  
  // Basically its a color updater
  private void generateColorWithLastChord() {
    this.chordType = this.chordType.replaceAll("\\s+", "");
    int x = 0;
 
    switch(this.chordType) {
      case "maj":
        this.generatedColor = color(255, x, 0);
        break;
        
      case "sus":
        this.generatedColor = color(x, 255, 0);
        break;
        
      case "mindom7":
        this.generatedColor = color(0, 255, x);
        break;
        
      case "min":
        this.generatedColor = color(0, x, 255);
        break;
        
      case "majdom7":
        this.generatedColor = color(x, 0, 255);
        break;

      case "majmaj7":
        this.generatedColor = color(255, 0, x);
        break;
    }  
  }
  
  // need to call generateColorWithLastChord() before calling this or the palette will be generated from the same starting color
   private color[] generatePaletteWithLastChord(){
     // for each color in palette
     for(int i=0; i<this.paletteLength; i++){
       // calculate random rgb mixing values
       int randRed = round(random(64, 255));
       int randGreen = round(random(64, 255));
       int randBlue = round(random(64, 255));
       
       // get original color rbg
       int red = this.generatedColor >> 16 & 0xFF; 
       int green = this.generatedColor >> 8 & 0xFF;
       int blue = this.generatedColor & 0xFF;

        red = (red + randRed)/2;
        green = (green + randGreen) / 2;
        blue = (blue + randBlue) / 2;
        
        // set new color in palette
        this.palette[i] = color(red, green, blue);
     }
     return this.palette;    
  }
  
  public void generatePalette(String chordType){
    
    // todo: arriva Chord type: A#mindom7, non correttamente parsato!!!!!!!!1
    if(chordType == "alt"){
      println ("AAAAAAAAAAAAAAAAALLLTLTTTTT-T--T-T-----");
     this.palette = new color[]{
        color(8, 24, 58),
        color(21, 40, 82),
        color(75, 61, 96),
        color(253, 94, 83),
        color(239, 84, 17),
        color(252, 156, 84),
        color(255, 227, 115),
        color(254, 76, 1),
        color(142, 199, 210),
        color(13, 105, 134),
        color(0, 150, 136),
        color(4, 117, 111)
      };
    } else {
      this.setChordType(chordType);
      this.generateColorWithLastChord();
      this.generatePaletteWithLastChord();
    }
  }
}

public class ColorIntepolator {
  public color interpolate(color oldColor, color newColor, float interpFactor){
    int interpRed = round(lerp(oldColor >> 16 & 0xFF, newColor >> 16 & 0xFF, interpFactor));
    int interpGreen = round(lerp(oldColor >> 8 & 0xFF, newColor >> 8 & 0xFF, interpFactor));
    int interpBlue = round(lerp(oldColor & 0xFF & 0xFF, newColor & 0xFF, interpFactor));
    return color(interpRed, interpGreen, interpBlue);    
  }
}

public class ChordRecogniser {
    
  private static final int INTERVALSUS2 = 2;
  private static final int INTERVALMIN3 = 3;
  private static final int INTERVALMAJ3 = 4;
  private static final int INTERVALSUS4 = 5;
  private static final int INTERVALDIM5 = 6;
  private static final int INTERVALPERF5 = 7;
  private static final int INTERVALMAJ5 = 8;
  private static final int INTERVAL6 = 9;
  private static final int INTERVALDOM7 = 10;
  private static final int INTERVALMAJ7 = 11;
  private String notes = "C C#D D#E F F#G G#A A#B ";
  private Integer octave;
  private String note;
  
  public String intervalsToChordType(Integer[] semitonesIntervals){
    String chordName = new String();
    
    // Third recognition
    if(Arrays.asList(semitonesIntervals).contains(INTERVALMIN3)){
      chordName += "min";
    } else if(Arrays.asList(semitonesIntervals).contains(INTERVALMAJ3)){
      chordName += "maj";
    } else {
      chordName += "sus";
    }
     
    // Seventh recognition
    if(Arrays.asList(semitonesIntervals).contains(INTERVALDOM7)){
      chordName += "dom7";
    } else if(Arrays.asList(semitonesIntervals).contains(INTERVALMAJ7)){
      chordName += "maj7";
    }
    
    if(Arrays.asList(semitonesIntervals).contains(INTERVALDIM5) || Arrays.asList(semitonesIntervals).contains(INTERVALMAJ5)){
      chordName = "alt";
    }
    return chordName;
  }
  
  public String midiFreq2Note(int noteNum){
    octave = noteNum / 12 - 1;
    note = notes.substring((noteNum % 12) * 2, (noteNum % 12) * 2 + 2); 
    note = note.replaceAll("\\s+","");
    return note;
  }
  
  public String recogniseChord(int root, int[] notes){
    Integer[] semitonesIntervals = new Integer[notes.length];
    for(int i= 0; i<notes.length; i++){
      semitonesIntervals[i] = (notes[i] - root%12)%12;
    }
    return midiFreq2Note(root) + intervalsToChordType(semitonesIntervals);
  }
  
    public String recogniseChordType(int root, int[] notes){
    Integer[] semitonesIntervals = new Integer[notes.length];
    for(int i= 0; i<notes.length; i++){
      semitonesIntervals[i] = (notes[i] - root%12)%12;
    }
    return intervalsToChordType(semitonesIntervals);
  }
  
}

public class OscHandler {
 
  public boolean isOscMessageNoteOn(OscMessage inOscMessage){
    return round(inOscMessage.get(0).floatValue()) == 1;
  }
  
  public int getRoot(OscMessage inOscMessage){
    return round(inOscMessage.get(1).floatValue()); 
  }
  
  public int[] getHarmonizedNotes(OscMessage inOscMessage){  
    MyArrayUtils myArrayUtils = new MyArrayUtils();
    int[] armonizedVoicesMidiNotes;
    
    // get harmonised notes midi number
    byte[] armonizedNotesBlob = inOscMessage.get(2).blobValue();
      try {
      // read blob
      Blob blob = new SerialBlob(armonizedNotesBlob);
      int blobLength = (int) blob.length();  
      byte[] blobAsBytes = blob.getBytes(1, blobLength);

      // init needed variables 
      // we dont know the number of harmonized voices until we parse the blob, but we know max size
      int[] maxPossibleArmonizedNotes = new int[blobLength/intByteLength]; 
      ByteBuffer buffer = ByteBuffer.wrap(blobAsBytes);
      int parsedVoiceMidiNumber = 0;
      
      // parse each blob element and store it in a int array
      for(int i=0; i<blobLength/intByteLength; i++){
        parsedVoiceMidiNumber = floor(buffer.getFloat());
        if(parsedVoiceMidiNumber > 0){
          maxPossibleArmonizedNotes[i] = parsedVoiceMidiNumber;
        }
      }
      armonizedVoicesMidiNotes = myArrayUtils.removeZeroesFromIntArray(maxPossibleArmonizedNotes);
     } catch(Exception e) {
     println("Blob error or parsing error"); 
     return new int[]{0, 0};
   }
   return armonizedVoicesMidiNotes;
  }
}

public class MyArrayUtils {
  
  public int[] removeZeroesFromIntArray(int[] array){
    int targetIndex = 0;
    for( int sourceIndex = 0;  sourceIndex < array.length;  sourceIndex++ )
    {
        if( array[sourceIndex] != 0 )
            array[targetIndex++] = array[sourceIndex];
    }
    int[] newArray = new int[targetIndex];
    System.arraycopy( array, 0, newArray, 0, targetIndex );
    return newArray;
  }
}
  
