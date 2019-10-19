import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Stack;
import java.io.File;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class NotationTree extends JComponent implements ActionListener, MouseListener {
	private static NotationTree self;
	private static int WIDTH = 1000;
	private static int HEIGHT = 600;
	private JFrame frame;
	private JLabel label;
	private JLabel value;
	private JTextField field;
	private JRadioButton infix;
	private JRadioButton prefix;
	private JRadioButton postfix;
	private ButtonGroup bg;
	private int mode = 1;

	private Node<Postfix> top;
	private Node<Postfix> oldSelected;

	public NotationTree() {
		this.self = this;
		setup();
	}

	private Queue<Postfix> infixToPostfix(String input) throws Exception {

		Stack<Character> operators = new Stack<>();
		Queue<Postfix> queue = new Queue<>();
		char c[] = input.toCharArray();

		for (int i = 0; i < c.length; i++) {
			if (c[i] == ' ') {
				continue;
			}

			// If the character is a number, it will look for trailing numbers
			if (isNum(c[i])) {
				StringBuilder sb = new StringBuilder("");
				while (i < c.length && isNum(c[i])) {
					sb.append(c[i++]);
				}
				int n = Integer.parseInt(sb.toString());
				Postfix pf = new Postfix(n);
				queue.enqueue(pf);
				i--;
				continue;
			}

			// If the character is a operator, it will check for presense for other
			// operators
			if (isOp(c[i])) {
				while (!operators.isEmpty() && operators.peek() != '(' && pres(c[i], operators.peek()) <= 0) {
					char x = operators.pop();
					Postfix pf = new Postfix(true, x);
					queue.enqueue(pf);
				}
				operators.push(c[i]);
			}

			if (c[i] == '(') {
				operators.push(c[i]);
			}

			// Properly takes care of the parenthesis
			if (c[i] == ')') {
				boolean found = false;
				while (!operators.isEmpty() && !(found = operators.peek() == '(')) {
					char x = operators.pop();
					Postfix pf = new Postfix(true, x);
					queue.enqueue(pf);
				}
				if (!found) {
					throw new Exception("Missing a opening parenthesis");
				}
				operators.pop();
			}
		}

		while (!operators.isEmpty()) {
			if (isOp(operators.peek())) {
				Postfix pf = new Postfix(true, operators.pop());
				queue.enqueue(pf);
			} else {
				throw new Exception("Closing parenthesis missing");
			}
		}

		return queue;
	}

	private int pres(char f, char t) {
		int fn;
		if (f == '(' || f == ')')
			fn = 3;
		else if (f == 'x' || f == '*' || f == '/')
			fn = 2;
		else if (f == '+' || f == '-')
			fn = 1;
		else
			fn = 0;

		int tn;
		if (t == '(' || t == ')')
			tn = 3;
		else if (t == 'x' || t == '*' || t == '/')
			tn = 2;
		else if (t == '+' || t == '-')
			tn = 1;
		else
			tn = 0;

		if (fn > tn)
			return 1;
		if (fn < tn)
			return -1;
		else
			return 0;
	}

	private boolean isOp(char c) {
		return c == 'x' || c == '*' || c == '/' || c == '+' || c == '-';
	}

	private boolean isNum(char c) {
		return c >= 48 && c <= 57;
	}

	private void evaulate(boolean postfix, Queue<Postfix> pf) {
		try {
			
			Stack<Node<Postfix>> stack = new Stack<>();
			while (!pf.empty()) {
				Postfix p = pf.dequeue();
				Node<Postfix> n = new Node<Postfix>(p);
				if (p.isOperator) {
					if (stack.size() < 2) {
						throw new Exception("Invalid Expression ~ missing integers");
					} else {
						if (postfix) {
							n.right = stack.pop();
							n.left = stack.pop();
						
						}else {
							n.left = stack.pop();
							n.right = stack.pop();
						}
					}
				}
				stack.push(n);
			}
			Node<Postfix> root = stack.pop();
			top = root;
			repaint();
		} catch (Exception e) {
			if (field.getText().length() == 0) {
				top = null;
			}
			repaint();
		}
	}

	public double valueOf(double left, Node<Postfix> op, double right) {
		
		char o = op.data.operator;
		
		if (o == 'x' || o == '*') {
			return left*right;
		}
		
		if (o == '/') {
			return left/right;
		}
		
		if (o == '+') {
			return left+right;
		}
		
		return left-right;
	}
	
	public double calculate(Node<Postfix> n) {
		if (n.left == null && n.right == null) {
			return n.data.number;
		}
		return valueOf(calculate(n.left),n,calculate(n.right));
	}
	
	public int findHeight(Node node) {
		if (node == null)
			return 0;
		return 1 + max(findHeight(node.left), findHeight(node.right));
	}

	public int max(int left, int right) {
		return left > right ? left : right;
	}

	public void paintTree(Graphics g, int minX, int maxX, int y, int yStep, Node tree) {

		String s = tree.data.toString();

		g.setFont(new Font("Roman", 0, 20));
		FontMetrics fm = g.getFontMetrics();
		int width = fm.stringWidth(s);
		int height = fm.getHeight();

		int cx = (minX + maxX) / 2;
		int cy = y + yStep / 2 - height / 3;
		
		if (tree.isSelected){
			g.setColor(Color.RED);
		}
		else if (tree.left != null && tree.right != null)
			g.setColor(Color.YELLOW);
		else
			g.setColor(Color.GREEN);
		
		
		g.fillOval(cx - 20, cy - 20, 40, 40);

		int xSep = Math.min((maxX - minX) / 8, 10);

		g.setColor(Color.BLACK);
		g.drawString(s, (minX + maxX) / 2 - width / 2, y + yStep / 2);

		tree.xpos = cx;
		tree.ypos = cy;
		
		if (tree.left != null) {
			int dx = (minX + maxX) / 2 - xSep;
			int dy = y + yStep / 2 + 5;
			int lx = (minX + (minX + maxX) / 2) / 2;
			int ly = y + yStep + yStep / 2 - height;
			g.drawLine(dx,dy, lx, ly);
			paintTree(g, minX, (minX + maxX) / 2, y + yStep, yStep, tree.left);
		}
		if (tree.right != null) {
			int dx = (minX + maxX) / 2 + xSep;
			int dy = y + yStep / 2 + 5;
			int lx = (maxX + (minX + maxX) / 2) / 2;
			int ly = y + yStep + yStep / 2 - height;
			g.drawLine(dx,dy,lx,ly);
			paintTree(g, (minX + maxX) / 2, maxX, y + yStep, yStep, tree.right);
		}
	}

	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		g.setColor(Color.WHITE);
		g.fillRect(0, 0, WIDTH, HEIGHT);

		if (top != null) {
			paintTree(g, 0, WIDTH, 50, HEIGHT / (findHeight(top) + 1), top);
		}
	}


	public Node<Postfix> getClosest(Node<Postfix> tree, int x, int y) {

		Queue<Node> queue = new Queue<>();
		
		if (tree == null) {
			return null;
		}
		
		int smallest = getDist(tree,x,y);
		Node<Postfix> closest = tree;
		
		try {
			queue.enqueue(tree);

			while (!queue.empty()) {
				
				Node<Postfix> n = queue.dequeue();
				if (n != null) {
					if (n.left != null) {
						queue.enqueue(n.left);
					}
					if (n.right != null) {
						queue.enqueue(n.right);
					}
					
					int dist = getDist(n,x,y);
					
					if (dist < smallest) {
						smallest = dist;
						closest = n;
					}
				}
				
			}
		} catch (Exception e) {

		}
		
		if (smallest < 25) {
			return closest;
		}
		
		return null;
	}
	
	public int getDist(Node<Postfix> n, int x, int y) {
		return (int) Math.sqrt((n.xpos-x)*(n.xpos-x) + (n.ypos-y)*(n.ypos-y));	
	}

	public void doInfix() {
		try {
			Queue<Postfix> queue = infixToPostfix(field.getText());
			self.evaulate(true,queue);
			repaint();
		} catch (Exception e) {
		}
	}
	
	public void doPrefix() {
		try {
			Stack<Postfix> stack = new Stack<>();
			char[] x = field.getText().toCharArray();
			for (int i = 0; i < x.length; i++) {
				if (x[i] == ' ') {
					continue;
				}
				if (isNum(x[i])) {
					Postfix p = new Postfix(Integer.parseInt(x[i]+""));
					stack.push(p);
				}
				
				if (isOp(x[i])) {
					Postfix p = new Postfix(true,x[i]);
					stack.push(p);
				}
			}
			Queue<Postfix> queue = new Queue<>();
			int len = stack.size();
			for (int i = 0; i < len; i++) {
				Postfix p = stack.pop();
				System.out.println(p.toString());
				
				queue.enqueue(p);
				
			}
			self.evaulate(false,queue);
			repaint();
		}catch(Exception e) {
			top = null;
			repaint();
		}
	}
	
	public void doPostfix() {
		try {
			Queue<Postfix> queue = new Queue<>();
			char[] x = field.getText().toCharArray();
			for (int i = 0; i < x.length; i++) {
				if (x[i] == ' ') {
					continue;
				}

				if (isNum(x[i])) {
					Postfix p = new Postfix(Integer.parseInt(x[i] + ""));
					queue.enqueue(p);
				}

				if (isOp(x[i])) {
					Postfix p = new Postfix(true, x[i]);
					queue.enqueue(p);
				}
			}
			self.evaulate(true,queue);
			repaint();
		} catch (Exception e) {
			if (field.getText().length() == 0) {
				top = null;
				repaint();
			}else {
				repaint();
			}
		}
	}

	public void setup() {
		frame = new JFrame("CS 313 Project");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setPreferredSize(new Dimension(WIDTH, HEIGHT));
		frame.getContentPane().add(this);
		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.addMouseListener(this);

		label = new JLabel("Expression:");
		label.setSize(125, 50);
		label.setFont(new Font("Ariel", Font.PLAIN, 20));
		label.setLocation(0, 0);
		add(label);

		value = new JLabel("Value: ");
		value.setSize(250, 50);
		value.setFont(new Font("Ariel", Font.PLAIN, 20));
		value.setLocation(500, HEIGHT - 75);
		add(value);

		field = new JTextField("");
		field.setSize(WIDTH - label.getWidth(), 50);
		field.setLocation(label.getWidth(), 0);
		field.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
				onAction();
			}

			public void removeUpdate(DocumentEvent e) {
				onAction();
			}

			public void insertUpdate(DocumentEvent e) {
				onAction();
			}

			public void onAction() {
				if (mode == 1) {
					doInfix();
				}
				if (mode == 3) {
					doPostfix();
				}
				if (mode == 2) {
					doPrefix();
				}
			}

		});
		add(field);

		infix = new JRadioButton();
		infix.setBounds(0, HEIGHT - 75, 150, 50);
		infix.setText("Infix Notation");
		infix.addActionListener(this);
		infix.doClick();
		add(infix);

		prefix = new JRadioButton();
		prefix.setBounds(150, HEIGHT - 75, 150, 50);
		prefix.setText("Prefix Notation");
		prefix.addActionListener(this);
		add(prefix);

		postfix = new JRadioButton();
		postfix.setBounds(300, HEIGHT - 75, 150, 50);
		postfix.setText("PostFix Notation");
		postfix.addActionListener(this);
		add(postfix);

		bg = new ButtonGroup();
		bg.add(infix);
		bg.add(postfix);
		bg.add(prefix);

		frame.pack();
		frame.setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(infix)) {
			mode = 1;
			try {
				self.evaulate(true,infixToPostfix(field.getText()));
				repaint();
			}catch(Exception ex) {
				top = null;
				repaint();
			}
		}
		if (e.getSource().equals(prefix)) {
			mode = 2;
			doPrefix();
			repaint();
		}
		if (e.getSource().equals(postfix)) {
			mode = 3;
			doPostfix();
			repaint();
		}
	}

	public static void main(String[] args) {
		new Thread() {
			public void run() {
				System.out.println((new File("")).getAbsolutePath());
				NotationTree et = new NotationTree();
			}
		}.start();
	}

	/**
	 * 
	 * Copy and Pasted the queue class from the website
	 * 
	 * @author CS 313
	 *
	 * @param <T>
	 */
	private class Queue<T> {
		private T data[];
		private int front, rear, size, capacity;

		@SuppressWarnings("unchecked")
		public Queue() {
			capacity = 100;
			data = (T[]) new Object[capacity];
			front = rear = size = 0;
		}

		public int size() {
			return size;
		}

		public boolean empty() {
			return size == 0;
		}

		public void enqueue(T x) throws Exception {
			if (size == capacity)
				throw new Exception("Queue<T> is full");
			data[rear++] = x;
			if (rear == capacity)
				rear = 0;
			size++;
		}

		public T dequeue() throws Exception {
			if (empty())
				throw new Exception("Queue is empty");
			T answer = (T) data[front++];
			if (front == capacity)
				front = 0;
			size--;
			return answer;
		}
	}

	private class Postfix {
		private boolean isOperator;
		private char operator;
		private int number;

		public Postfix(boolean isOperator, char operator) {
			this.isOperator = isOperator;
			this.operator = operator;
		}

		public Postfix(int number) {
			this.isOperator = false;
			this.number = number;
		}

		public String toString() {
			if (isOperator)
				return operator + "";
			else
				return number + "";
		}
	}

	private class Node<T> {
		private Node left, right;
		private int xpos, ypos;
		private T data;
		private boolean isSelected;

		public Node(T data) {
			this.data = data;
			isSelected =false;
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		Node<Postfix> n = getClosest(top,e.getX(),e.getY()-20);

		if (oldSelected != null && !oldSelected.equals(n)) {
			oldSelected.isSelected = false;
		}
		
		oldSelected = n;
		
		if (n != null) {
			if (n.isSelected) {
				n.isSelected = false;
				value.setText("Value: ");
			}else {
				n.isSelected = true;
				double x = calculate(n);
				value.setText("Value: "+x);
			}
		}
		repaint();
	}

	public void mouseReleased(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {}

}
