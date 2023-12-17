import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.ImageIcon;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

public class MusicPlayerFrame extends JFrame {

    private VolumeChangeListener volumeChangeListener = null;
    private Clip clip = null;
    private JButton playBtn;
    private JTextField mediaPathTxt;
    private VolumeSlider slider;



    public static void main(String[] args) {
        new MusicPlayerFrame();
    }

    public MusicPlayerFrame() {
        super("MusicPlayer");

        this.setSize(360, 640);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setResizable(false);

        ImageIcon icon = new ImageIcon("Icon.png");
        setIconImage(icon.getImage());

        JPanel p = new JPanel();
        p.setLayout(null);



        playBtn = new JButton("再生");
        playBtn.setBounds(130, 450, 80, 40);
        playBtn.setFont(new Font(playBtn.getFont().getFontName(), Font.PLAIN, 15));
        playBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (mediaPathTxt.getText().isEmpty()) {
                    JOptionPane.showMessageDialog(
                            MusicPlayerFrame.this, "音声ファイルを選択してください。", "エラー",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                JButton source = (JButton) e.getSource();
                if (source.getText().equals("再生")) {
                    PlayWorker worker = new PlayWorker();
                    worker.execute();
                    return;
                }
                StopWorker worker = new StopWorker();
                worker.execute();
            }
        });

        JButton fileBtn = new JButton("参照");
        fileBtn.setBounds(20, 500, 60, 30);
        fileBtn.setFont(new Font(fileBtn.getFont().getFontName(), Font.PLAIN, 12));
        fileBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                FileFilter filter = new FileNameExtensionFilter("wavファイル(.wav)", "wav");
                chooser.setSelectedFile(new File(mediaPathTxt.getText()));
                chooser.setFileFilter(filter);
                int selected = chooser.showOpenDialog(MusicPlayerFrame.this);
                if (selected == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    mediaPathTxt.setText(file.getAbsolutePath());
                }
            }
        });

        mediaPathTxt = new JTextField();
        mediaPathTxt.setBounds(100, 500, 230, 30);

        slider = new VolumeSlider();
        slider.setBounds(160, 20, 180, 40);
        p.add(playBtn);
        p.add(mediaPathTxt);
        p.add(fileBtn);
        p.add(slider);
        this.getContentPane().add(p);
        this.setVisible(true);
    }

    class PlayWorker extends SwingWorker {
        private AudioInputStream stream = null;
        @Override
        protected Void doInBackground() throws Exception {
            AudioFormat format = null;
            try {
                playBtn.setText("停止");
                stream = AudioSystem.getAudioInputStream(new File(mediaPathTxt.getText()));

                format = stream.getFormat();
                DataLine.Info cInfo = new DataLine.Info(Clip.class, format);

                if (!AudioSystem.isLineSupported(cInfo)) {
                    return null;
                }

                clip = (Clip) AudioSystem.getLine(cInfo);
                clip.open(stream);
                float volume = (float) ((float)slider.getValue() / 100f);
                FloatControl ctrl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                ctrl.setValue((float) Math.log10(volume) * 20);
                volumeChangeListener = new VolumeChangeListener();
                clip.addLineListener(volumeChangeListener);
                clip.start();
                clip.flush();
                while (clip.isActive())
                    Thread.sleep(1);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public void done() {
            try {
                playBtn.setText("再生");
            } finally {
                if (clip != null && clip.isOpen())
                    clip.close();
                try {
                    if(stream != null)
                        stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class StopWorker extends SwingWorker {
        @Override
        protected Void doInBackground() throws Exception {
            playBtn.setText("再生");
            if (clip != null && clip.isOpen()) {
                clip.stop();
                clip.flush();
            }
            return null;
        }
        @Override
        public void done() {
            playBtn.setText("再生");
        }
    }

    class VolumeSlider extends JSlider {
        public VolumeSlider() {
            super(0, 100, 25);
            this.setMajorTickSpacing(25);
            this.setPaintLabels(true);
            this.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    if (volumeChangeListener != null)
                        volumeChangeListener.fireVolumeChange();
                }
            });
        }
    }

    class VolumeChangeListener implements LineListener {
        public void fireVolumeChange() {
            float volume = (float) ((float)slider.getValue() / 100f);
            FloatControl ctrl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            ctrl.setValue((float)Math.log10(volume) * 20);
        }
        @Override
        public void update(LineEvent event) {}
    }
}

