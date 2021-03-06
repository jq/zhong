package music.gui;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Random;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import util.Constants;

import music.info.MusicInfo;
import music.threads.ImageThread;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * ImageDialog.java
 *
 * Created on Nov 14, 2010, 10:03:17 AM
 */

/**
 *
 * @author liutao
 */
public class ImageDialog extends javax.swing.JDialog implements ActionListener
{
	private MusicInfo music;
	private Image[] images;
	private JButton[] buttons;
	private int current_select;
	
    /** Creates new form ImageDialog */
    public ImageDialog(java.awt.Frame parent, boolean modal, Image[] imgs, MusicInfo info) {
        super(parent, modal);
        initComponents();
        
        
        images = imgs;
        music = info;
        
        setTitle(music.getTitle());
        //titleLabel.setText(music.getTitle());
    	buttons = new JButton[Constants.IMAGE_NUM];
        buttons[0] = jButton4;  
        buttons[1] = jButton5;
        buttons[2] = jButton6;
        for(int i=0; i<Constants.IMAGE_NUM; i++)
        	buttons[i].setIcon(new ImageIcon(images[i]));
        
        current_select = 0;
        imagePanel.setImage(images[current_select]);
        
        setLocation((int)(Math.random()*Constants.WINDOW_SIZE), (int)(Math.random()*Constants.WINDOW_SIZE));
        setVisible(true);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">
    private void initComponents() {

        titleLabel = new javax.swing.JLabel();
        imagePanel = new MyPanel();
        leftButton = new javax.swing.JButton();
        okButton = new javax.swing.JButton();
        rightButton = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        addWindowFocusListener(new WindowAdapter()
		{
        	public void windowClosing(WindowEvent e)
        	{
        		 
        		 music.doCancel();
 			     dispose();
 		    }
		});

        leftButton.setFont(new java.awt.Font("DejaVu Sans", 0, 10)); // NOI18N
        leftButton.setText("<<");
        leftButton.addActionListener(this);

        okButton.setFont(new java.awt.Font("DejaVu Sans", 0, 10)); // NOI18N
        okButton.setText("OK");
        okButton.addActionListener(this);
        
        rightButton.setFont(new java.awt.Font("DejaVu Sans", 0, 10)); // NOI18N
        rightButton.setText(">>");
        rightButton.addActionListener(this);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(titleLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jButton5, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jButton6, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(64, 64, 64)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(imagePanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                .addComponent(leftButton, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(rightButton, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addContainerGap(24, Short.MAX_VALUE))
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {okButton, rightButton, leftButton});

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jButton4, jButton5, jButton6});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(titleLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(imagePanel, javax.swing.GroupLayout.PREFERRED_SIZE, 132, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(leftButton)
                    .addComponent(okButton)
                    .addComponent(rightButton, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton4, javax.swing.GroupLayout.DEFAULT_SIZE, 77, Short.MAX_VALUE)
                    .addComponent(jButton5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButton6))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {okButton, rightButton, leftButton});

        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jButton4, jButton5, jButton6});

        pack();
    }// </editor-fold>

    private void leftButtonActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
               // ImageDialog dialog = new ImageDialog(new javax.swing.JFrame(), true);
            }
        });
    }

    // Variables declaration - do not modify
    private MyPanel imagePanel;
    private javax.swing.JButton okButton;
    private javax.swing.JButton rightButton;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton leftButton;
    private javax.swing.JLabel titleLabel;
    // End of variables declaration
    
	@Override
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == leftButton)
		{
			if(current_select > 0)
			{
				current_select --;
				imagePanel.setImage(images[current_select]);
			}
		}
		else if(e.getSource() == okButton)
		{
			myDispose();
		}
		else if(e.getSource() == rightButton)
		{
			if(current_select < Constants.IMAGE_NUM-1)
			{
				current_select ++;
				imagePanel.setImage(images[current_select]);
			}
		}
			
	}
	
	
	public void myDispose()
	{
		music.setImageName(music.getTitle()+"_img.jpg");
		if(ImageThread.storeImage(images[current_select], Constants.DOWNLOAD_DIR+music.getImageName()))
			((MyFrame)getParent()).showMessage(music.getTitle()+" image download success!");
		else 
			((MyFrame)getParent()).showMessage(music.getTitle()+" image store error!");
			
		this.dispose();
	}
}
