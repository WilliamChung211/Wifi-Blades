import java.net.*;
import java.util.*;


import java.io.*;

/*
 * Name: William Chung and Eric S.
 * 
 * This program represents a server sending
 * messages about the game from each player
 */
public class Server {

	private Scanner[] readers;
	private PrintWriter[] writers;
	private int playerTurn; // [0,1]

	public Server() {

		readers = new Scanner[2];
		writers = new PrintWriter[2];
		playGame();
		
	}

	private int draw(){

		int firNum = ((int)(Math.random()*6)+2);
		
		int secNum;
		do{
			secNum = ((int)(Math.random()*6)+2);
		} while(secNum == firNum);
		
		writers[0].println(firNum + "," + secNum);
		writers[0].flush();
		writers[1].println(secNum + "," + firNum);
		writers[1].flush();
		
		if(firNum<secNum) {
			return 0;
		}
		else {
			return 1;
		}
	
		
	}
	
	public void playGame() {

		try {
			// Set up the server socket, print IP and port info
			ServerSocket server = new ServerSocket(4242);
			System.out.println(server.getLocalPort());
			System.out.println(InetAddress.getLocalHost().getHostAddress());

			Socket[] socks = new Socket[2];
			// Accept two players and set up the readers and writers
			for(int i =0;i<2;i++) {
				socks[i]=server.accept();
				readers[i] = new Scanner(socks[i].getInputStream());
				writers[i] = new PrintWriter(socks[i].getOutputStream());
			}
		
			
			// generates decks and sends them
			String []decks = new String[2];
			for(int player =0;player<2;player++) {
				decks[player]="";
				for (int i = 0; i <  10; i++) {
					int card = (int) (Math.random() * 8);
					decks[player]+=card + ",";
				}

				decks[player]=decks[player].substring(0,decks[player].length()-1);
				writers[player].println(decks[player]);
				writers[player].flush();
	
			}
			writers[1].println(decks[0]);
			writers[1].flush();
			writers[0].println(decks[1]);
			writers[0].flush();
			// generates active cards, determines who goes first
			// and sends them

			
			int turnInd = draw();
			

			
			String result = "";
			int otherPlayInd;
			// gets and sends messages till the game is over
			do {

				otherPlayInd =(turnInd+1) %2;
				result = readers[turnInd].nextLine();

				if(result.contains("GO")) {
					writers[otherPlayInd].println(result);
					writers[otherPlayInd].flush();
					turnInd = otherPlayInd;
				}
				else if (result.contains("REDRAW")){
					
					//gives the player who initiatied the redraw the message to not change anything but the active cards
					writers[turnInd].println("REDRAW,-1");
					writers[turnInd].flush();
					
					writers[otherPlayInd].println(result);
					writers[otherPlayInd].flush();
					
					//sends the cards
					turnInd = draw();
				
				}
				
			} while (!result.contains("WIN"));
			
			// sends final message to losing player
			writers[otherPlayInd].println(result);
			writers[otherPlayInd].flush();
			writers[turnInd].println("LOSE,-1");
			writers[turnInd].flush();
			
			//closes everything when done
			for(int i=0;i<2;i++) {
				readers[i].close();
				writers[i].close();
				socks[i].close();
			}
			

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new Server();
	}
}
