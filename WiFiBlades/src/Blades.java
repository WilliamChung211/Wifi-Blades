import javax.imageio.ImageIO;
import javax.swing.*;

import java.io.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.*;
import java.net.*;

/*
 * Name: William Chung and Eric S
 * 
 * This represents a client playing a game of blades
 * sending and recieving messages with a GUI and thread.
 */
public class Blades extends JFrame {

	private Player player;
	private Player opponent;

	private boolean playerTurn; // true if it's the player's turn

	private BufferedImage boltImage;
	private BufferedImage mirrorImage;

	private Socket socket;
	private Scanner reader;
	private PrintWriter writer;
	private final static String SERVER_IP = "10.104.7.250"; // fill this in

	public Blades(String ip, int port) {

		setTitle("Blades_Client");
		setSize(1100, 800);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		getContentPane().setBackground(Color.white);
		setLayout(null);

		try {
			boltImage = ImageIO.read(new File("bolt.jpg"));
			mirrorImage = ImageIO.read(new File("mirror.png"));

		} catch (IOException ioe) {
			JOptionPane.showMessageDialog(null, "Could not read in the pic");
			System.exit(0);
		}

		player = new Player(Color.blue, "You");
		opponent = new Player(Color.red, "Opp.");

		try {
			socket = new Socket(ip, port);
			reader = new Scanner(socket.getInputStream());
			writer = new PrintWriter(socket.getOutputStream());
		} catch (IOException e) {

			e.printStackTrace();
		}

		// gets the cards from the server
		String[] playerCards;
		String result = reader.nextLine();
		playerCards = result.split(",");
		String []oppCards;
		String result2 = reader.nextLine();
		oppCards = result2.split(",");
		
		JPanel oppPanel = new JPanel();
		oppPanel.setLayout(new GridLayout(1, 10, 5, 5));
		oppPanel.setBackground(Color.black);
		oppPanel.setBorder(BorderFactory.createLineBorder(Color.black, 5));
		oppPanel.setBounds(75, 75, 925, 125);
		int cardInd = 0;
		
		//loads the cards to the GUI
		for (NumPanel next : opponent.cards) {

			next.setNumber(Integer.parseInt(oppCards[cardInd]));
			cardInd++;

			oppPanel.add(next);
			next.removeMouseListener(next); 
		}

		add(oppPanel);

		JPanel playerPanel = new JPanel();
		playerPanel.setLayout(new GridLayout(1, 10, 5, 5));
		playerPanel.setBackground(Color.black);
		playerPanel.setBorder(BorderFactory.createLineBorder(Color.black, 5));
		playerPanel.setBounds(75, 550, 925, 125);

		cardInd = 0;
		for (NumPanel next : player.cards) {

			next.setNumber(Integer.parseInt(playerCards[cardInd]));
			cardInd++;

			playerPanel.add(next);
		}
		
		add(playerPanel);

		player.activeCard = new NumPanel(Color.blue, -1);
		player.activeCard.setBorder(BorderFactory.createLineBorder(Color.black, 5));
		player.activeCard.setBounds(550, 375, 97, 125);
		player.activeCard.removeMouseListener(player.activeCard);
		add(player.activeCard);

		opponent.activeCard = new NumPanel(Color.red, -1);
		opponent.activeCard.setBorder(BorderFactory.createLineBorder(Color.black, 5));
		opponent.activeCard.setBounds(425, 250, 97, 125);
		opponent.activeCard.removeMouseListener(opponent.activeCard);
		add(opponent.activeCard);

		player.label.setBounds(675, 400, 100, 80);
		add(player.label);

		opponent.label.setBounds(355, 270, 100, 80);
		add(opponent.label);

		// reads in the active cards and determines who goes first
		goesFirst(reader.nextLine());

		Thread t = new Thread(new ClientHandler());
		t.start();

		setVisible(true);
	}


		
	// determine who goes first in the format "yourCard,oppCard"
	private void goesFirst(String line) {

		// receive two cards from the server
		String[] activeCards = line.split(",");
		int playerCard = Integer.parseInt(activeCards[0]);
		int oppCard = Integer.parseInt(activeCards[1]);

		player.activeCard.removeNumber();
		opponent.activeCard.removeNumber();
		player.activeCard.setNumber(playerCard);
		opponent.activeCard.setNumber(oppCard);

		if (playerCard < oppCard) {
			player.label.setForeground(Color.green);
			opponent.label.setForeground(Color.black);
			playerTurn = true;
		} else {
			opponent.label.setForeground(Color.green);
			player.label.setForeground(Color.black);
			playerTurn = false;
		}
	}


	//swaps green color
	private void swapTurns(){
			Color toSwap = player.label.getForeground();
			player.label.setForeground(opponent.label.getForeground());
			opponent.label.setForeground(toSwap);
			playerTurn = !playerTurn;
	}
	
	// updates active cards by calling the change method from our card based on who is doing the turn
	private void evaluateTurn(Player current, Player other, NumPanel selectedCard) {

		selectedCard.change(current, other);
		
		//swaps turns
		swapTurns();

	}

	//sends if the player lost and the opponent won
	private void playerLost (int index) {
		writer.println("WIN," + index);
		writer.flush();
		
	}


	//handles getting messages from the server and telling the GUI how to handle it
	public class ClientHandler implements Runnable {

		public void run() {

			String result = "";
			int move;

			do {

				// READS AND PARSES INPUT FROM SERVER
				String[]output =reader.nextLine().split(","); 
				result = output[0];
				move = Integer.parseInt(output[1]);

				// evaluates the go turn
				if (result.contains("GO")) {

					evaluateTurn(opponent, player, opponent.cards[move]);
					
					//if the player has no cards left and is losing, he lost
					if(player.cardsLeft==0&&opponent.activeCard.number>player.activeCard.number) {
						playerLost(-1);
					}


				} 

				// 
				else {
					
					//updates opponents deck
					if(move!=-1) {
						opponent.cards[move].removeNumber();
					}
					
					if (result.contains("REDRAW")) {

						JOptionPane.showMessageDialog(null, "Redraw");
						goesFirst(reader.nextLine());
					}
				
				}
				

			} while (!result.contains("LOSE") && !result.contains("WIN"));

		
		
			// DISPLAYS IF THEY WON OR LOST
			JOptionPane.showMessageDialog(null, "YOU " + result +"!!!!!");
			playerTurn = false;
			
			//closes everything when the game is over
			reader.close();
			writer.close();
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	// Represents a single player in the game.
	// You do not need to add anything to this class
	public class Player {

		private NumPanel[] cards; // deck of cards
		private NumPanel activeCard; // card in the middle
		private ArrayList<Integer> playedCards; // in the event of a Bolt
		private int cardsLeft;
		private JLabel label;

		public Player(Color theColor, String labelMessage) {

			label = new JLabel(labelMessage);
			label.setFont(new Font("Lucia Handwriting", Font.PLAIN, 22));
			cards = new NumPanel[10];

			for (int i = 0; i < 10; i++) {
				cards[i] = new NumPanel(theColor, i);

			}
			playedCards = new ArrayList<Integer>();
			cardsLeft = 10;
		}

	}

	// represents a single card panel
	class NumPanel extends JPanel implements MouseListener {

		private int number = -1; // 0 is for Bolt, 1 is for Mirror, -1 is empty
		private JLabel text; // helps draw a number on the card
		private Color backOfCard; // color when it's flipped over
		private int index;

		public NumPanel(Color t, int loc) {

			setBackground(Color.white);
			setLayout(null);
			backOfCard = t;
			index = loc;
			this.addMouseListener(this);
		}

		// changes the panel to have the given number
		public void setNumber(int number) {

			this.number = number;

			// if it's not bolt or mirror
			if (number != 0 && number != 1) {

				text = new JLabel("" + number, SwingConstants.CENTER);
				text.setFont(new Font("Calibri", Font.PLAIN, 55));
				text.setBounds(0, 35, 70, 60);
				this.add(text);
				this.setBackground(Color.white);

			}
			repaint();
		}

		public void removeNumber() {

			// if the card contains a number
			if (number != -1 && number != 0 && number != 1) {
				this.remove(text);
			}
			number = -1;
			repaint();
		}

		// called by repaint
		public void paintComponent(Graphics g) {
			super.paintComponent(g);

			if (number == -1)
				setBackground(backOfCard);
			else if (number == 0) {
				g.drawImage(boltImage, 10, 10, this);
			} else if (number == 1) {
				g.drawImage(mirrorImage, 5, 10, this);
			}
		}

		// reacts to a mouse click
		// carries out one play of Blades
		// YOU MUST MODIFY THIS METHOD
		public void mouseClicked(MouseEvent arg0) {

			// ONLY REACTS IF IT'S THE PLAYER'S TURN
			// EVALUATES THE PLAYER'S TURN

			// DETERMINES IF THEY SHOULD KEEP PLAYING
			// IF THE PLAYER LOST OR IF IT'S A REDRAW
			// SENDS APPROPRIATE MESSAGE TO SERVER
			if (playerTurn) {

				evaluateTurn(player, opponent, this);
				// if the cards were not redraw sit checks the loss
				if (!checkTie()) {
					checkLose();
				}
				
				removeMouseListener(this);
			}

			

		}

		private boolean checkTie() {

			if (player.activeCard.number == opponent.activeCard.number) {
				writer.println("REDRAW," + index);
				writer.flush();

				/*
				 * //if it is a tie, clears the played cards list and redraws new cards from
				 * 2-7. The player with lowest card gets turn
				 * JOptionPane.showMessageDialog(null, "Redraw");
				 * player.activeCard.removeNumber(); player.playedCards = new
				 * ArrayList<Integer>();
				 * 
				 * opponent.activeCard.removeNumber(); opponent.playedCards = new
				 * ArrayList<Integer>();
				 * 
				 * redraw();
				 */
				return true;

			}

			return false;
		}

		// checks if the turn player's cards are lower than the opponent player's cards
		// and prints out end message
		private void checkLose() {


			if (player.activeCard.number > opponent.activeCard.number) {
				writer.println("GO," + index);
				writer.flush();
			}
			else {
				playerLost(index);
			
	
			}



		}

		private void change(Player turnPlayer, Player oppPlayer) {

			// changes the player's card
			turnPlayer.cardsLeft--;

			// if it is a mirror it switches the player card's array list and active card
			if (number == 1) {

				int temp = oppPlayer.activeCard.number;
				setActiveCard(oppPlayer, turnPlayer.activeCard.number);

				ArrayList<Integer> oppCards = oppPlayer.playedCards;
				oppPlayer.playedCards = turnPlayer.playedCards;
				turnPlayer.playedCards = oppCards;

				setActiveCard(turnPlayer, temp);

				JOptionPane.showMessageDialog(null, "Mirror!");
			}

			// if it is a bolt, it gets rid of the not turn player's active card
			else if (number == 0) {
				ArrayList<Integer> cards = oppPlayer.playedCards;
				if (cards.size() > 0) {
					int numToRemove = cards.remove(cards.size() - 1);
					setActiveCard(oppPlayer, oppPlayer.activeCard.number - numToRemove);
				}

				JOptionPane.showMessageDialog(null, "Bolt!");
			}

			// if it is another card it adds that card to the active card
			else {
				turnPlayer.playedCards.add(number);
				setActiveCard(turnPlayer, turnPlayer.activeCard.number + number);
			}
			removeNumber();

		}

		// It sets the card to the number
		private void setActiveCard(Player turnPlayer, int numberToSet) {
			turnPlayer.activeCard.removeNumber();
			turnPlayer.activeCard.setNumber(numberToSet);
		}

		public void mouseEntered(MouseEvent arg0) {
			// do not implement

		}

		@Override
		public void mouseExited(MouseEvent arg0) {
			// do not implement

		}

		@Override
		public void mousePressed(MouseEvent arg0) {
			// do not implement
		}

		@Override
		public void mouseReleased(MouseEvent arg0) {
			// do not implement

		}
	}

	public static void main(String[] args) {
		new Blades(SERVER_IP, 4242);
	}
}





