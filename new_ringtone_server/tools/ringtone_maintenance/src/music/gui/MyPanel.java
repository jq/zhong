package music.gui;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Label;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;


public class MyPanel extends JPanel
{
	private  JLabel  label;
	
	public MyPanel()
	{
		label = new JLabel();
		add(label);
	}
	
	public void setImage(Image image)
	{
		label.setIcon(new ImageIcon(image));
		repaint();
	}
	
	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);	
	}
	
	
}
