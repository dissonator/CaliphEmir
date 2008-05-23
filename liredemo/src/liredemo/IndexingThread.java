package liredemo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.image.BufferedImage;

import net.semanticmetadata.lire.DocumentBuilder;
import net.semanticmetadata.lire.DocumentBuilderFactory;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifReader;
import com.drew.metadata.exif.ExifDirectory;
import com.drew.imaging.jpeg.JpegProcessingException;

import javax.imageio.ImageIO;

/*
 * This file is part of the Caliph and Emir project: http://www.SemanticMetadata.net.
 *
 * Caliph & Emir is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Caliph & Emir is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Caliph & Emir; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * Copyright statement:
 * --------------------
 * (c) 2002-2007 by Mathias Lux (mathias@juggle.at)
 * http://www.juggle.at, http://www.SemanticMetadata.net
 */

/**
 * This file is part of the Caliph and Emir project: http://www.SemanticMetadata.net
 *
 * @author Mathias Lux, mathias@juggle.at
 */
public class IndexingThread extends Thread {
    LireDemoFrame parent;
    /** Creates a new instance of IndexingThread
     * @param parent
     */
    public IndexingThread(LireDemoFrame parent) {
        this.parent = parent;
    }
    
    public void run() {
        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance();
        df.setMaximumFractionDigits(0);
        df.setMinimumFractionDigits(0);
        try {
            parent.progressBarIndexing.setValue(0);
            java.util.ArrayList<java.lang.String> images =
                    getAllImages(
                    new java.io.File(parent.textfieldIndexDir.getText()), true);
            IndexWriter iw = new IndexWriter(parent.textfieldIndexName.getText(), new SimpleAnalyzer(), !parent.checkBoxAddToExisintgIndex.isSelected());
            int builderIdx = parent.selectboxDocumentBuilder.getSelectedIndex();
            DocumentBuilder builder = DocumentBuilderFactory.getFullDocumentBuilder();
            int count = 0;
            long time = System.currentTimeMillis();
            for (String identifier : images) {
                try {
                    Document doc = builder.createDocument(readFile(identifier), identifier);
                    iw.addDocument(doc);
                } catch (Exception e) {
                    System.err.println("Could not add document " + identifier);
                    e.printStackTrace();
                }
                count++;
                float percentage = (float) count/ (float) images.size();
                parent.progressBarIndexing.setValue((int) Math.floor(100f*percentage));
                float msleft = (float) (System.currentTimeMillis() - time) / percentage;
                float secLeft = msleft * (1 - percentage) / 1000f;
                String toPaint = "~ " + df.format(secLeft) + " sec. left";
                if (secLeft>90) toPaint = "~ " + Math.ceil(secLeft/60) + " min. left";
                parent.progressBarIndexing.setString(toPaint);
            }
            long timeTaken = (System.currentTimeMillis() - time);
            float sec = ((float) timeTaken) / 1000f;
            
            parent.progressBarIndexing.setString(Math.round(sec) + " sec. for " + count + " files");
            parent.buttonStartIndexing.setEnabled(true);
            iw.optimize();
            iw.close();
            
        } catch (IOException ex) {
            Logger.getLogger("global").log(Level.SEVERE, null, ex);
        }
    }

    public static ArrayList<String> getAllImages(File directory, boolean descendIntoSubDirectories) throws IOException {
        ArrayList<String> resultList = new ArrayList<String>(256);
        File[] f = directory.listFiles();
        for (File file : f) {
            if (file != null && file.getName().toLowerCase().endsWith(".jpg") && !file.getName().startsWith("tn_")) {
                resultList.add(file.getCanonicalPath());
            }
            if (descendIntoSubDirectories && file.isDirectory()) {
                ArrayList<String> tmp = getAllImages(file, true);
                if (tmp != null) {
                    resultList.addAll(tmp);
                }
            }
        }
        if (resultList.size() > 0)
            return resultList;
        else
            return null;
    }

    private BufferedImage readFile(String path) throws IOException {
        BufferedImage image = null;
        FileInputStream jpegFile = new FileInputStream(path);
        Metadata metadata = new Metadata();
        try {
            new ExifReader(jpegFile).extract(metadata);
            byte[] thumb = ((ExifDirectory) metadata.getDirectory(ExifDirectory.class)).getThumbnailData();
            if (thumb!=null) image = ImageIO.read(new ByteArrayInputStream(thumb));
//            System.out.print("Read from thumbnail data ... ");
//            System.out.println(image.getWidth() + " x " + image.getHeight());
        } catch (JpegProcessingException e) {
            System.err.println("Could not extract thumbnail");
            e.printStackTrace();
        } catch (MetadataException e) {
            System.err.println("Could not extract thumbnail");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Could not extract thumbnail");
            e.printStackTrace();
        }
        // Fallback:
        if (image == null) image = ImageIO.read(new FileInputStream(path));
        return image;
    }

}