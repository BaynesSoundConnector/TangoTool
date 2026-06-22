package pkgForTTNew;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JOptionPane;

public class ExportXSPF
{
    public static boolean convertToXSPF(ArrayList<Track> tracks, String fileName, String musicBasePath)
    {
        int count = 0;
        try
        {
            HashMap<Character, String> map1 = SetUpTrans1();
            HashMap<Character, String> map2 = SetUpTrans2();
            File file = new File(musicBasePath+fileName);
            if (file.exists())
            {
                int rc = JOptionPane.showConfirmDialog(null, "Playlist file exists, overwrite?", fileName,
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (rc == JOptionPane.NO_OPTION)
                    return false;
            }
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            
            StringBuffer sb = new StringBuffer();
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            sb.append("<playlist xmlns=\"http://xspf.org/ns/0/\" xmlns:vlc=\"http://www.videolan.org/vlc/playlist/ns/0/\" version=\"1\">\n");
            //sb.append("<title>Playlist</title>\n");
            sb.append("<trackList>\n");
            Iterator<Track> it = tracks.iterator();
            while (it.hasNext())
            {
                
                Track tt = it.next();
                sb.append("<track>\n");
                sb.append("<location>");
                sb.append("file:///" + musicBasePath+"\\");
                sb.append(translate1(tt.relativePath, map1));
                sb.append("/");
                String nm = translate1(tt.fileName, map1);
                if (!nm.matches("\\A\\p{ASCII}*\\z"))
                {
                    Utilities.out("Invalid Character(fileName):"+nm);
                }
                sb.append(translate1(tt.fileName, map1));
                sb.append("</location>\n");
                if (tt.style == Track.Style.Cortina)
                    sb.append("<title>"+"(*)"+translate2(tt.title, map2)+"</title>\n");
                else if (tt.style == Track.Style.Tango)
                    sb.append("<title>"+"T-"+translate2(tt.title, map2)+"</title>\n");
                else if (tt.style == Track.Style.Milonga)
                    sb.append("<title>"+"M-"+translate2(tt.title, map2)+"</title>\n");
                else if (tt.style == Track.Style.Valse)
                    sb.append("<title>"+"V-"+translate2(tt.title, map2)+"</title>\n");
                else if (tt.style == Track.Style.AltTango)
                    sb.append("<title>"+"AT-"+translate2(tt.title, map2)+"</title>\n");
                else
                    sb.append("<title>"+translate2(tt.title, map2)+"</title>\n");
                //sb.append("<genre>Tango</genre>\n");
                //sb.append("<duration>");
                //String temp = Float.toString(tt.calculatedTime * 1000);
                //sb.append(temp.substring(0, temp.indexOf('.')));
                //sb.append("</duration>\n");

                //sb.append("<extension application=\"http://www.videolan.org/vlc/playlist/0\">\n");
                //sb.append("<vlc:id>" + (count++) + "</vlc:id>\n");
                //sb.append("</extension>\n");

                
                sb.append("</track>\n");
                bw.write(sb.toString());
                sb = new StringBuffer();
            }
            sb.append("</trackList>\n");
            //sb.append("<extension application=\"http://www.videolan.org/vlc/playlist/0\">\n");
            //for (int i = 0; i < count; i++)
            //{
            //  sb.append("<vlc:item tid=\""+i+"\"/>\n");
            //}
            //sb.append("</extension>\n");
            sb.append("</playlist>\n");
            bw.write(sb.toString());
            bw.close();
            return true;
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }
    public static HashMap<Character, String> SetUpTrans1()
    {
        HashMap<Character, String> map1 = new HashMap<Character, String>();

        // HashMap<String, String> map = new HashMap<String, String>();
        //map.put(' ', "%20");
        // map.put('+', "%2b");
        map1.put('#', "\u0023");
        map1.put('(', "%28");
        map1.put(')', "%29");
        map1.put('\'', "%27");
        map1.put('%', "%2b");
        map1.put('\u00a1', "%C2%A1"); // inverted exclamation
        map1.put('\u00ED', "%C3%AD"); // small i accute
        map1.put('\u00F3', "%C3%B3"); // small o accute
        map1.put('\u00F1', "%C3%B1"); // small n with tilde
        map1.put('\u00FA', "%C3%BA"); // small u with acute
        map1.put('\u00E9', "%C3%A9"); // small e with acute
        map1.put('\u00E1', "%C3%A1"); // small a with acute
        map1.put('\u00FC', "%C3%BC"); // small Letter u with diaeresis
        //map1.put('\u2018', "'"); // single left quotation
        map1.put('\u2018', "&#x2018;"); // single left quotation
        //map1.put('\u2019', "'"); // single right quotation
        map1.put('\u2019', "&#x2019;"); // single right quotation
        map1.put('&', "&amp;");
        return map1;
    }
    /**
     * A more forgiving translation for song titles. Not literal.
     */
    public static HashMap<Character, String> SetUpTrans2()
    {
        HashMap<Character, String> map2 = new HashMap<Character, String>();

        // HashMap<String, String> map = new HashMap<String, String>();
        //map.put(' ', "%20");
        // map.put('+', "%2b");
        //map2.put('(', "%28");
        //map2.put(')', "%29");
        //map2.put('(', "%28");
        //map2.put(')', "%29");
        //map2.put('\'', "%27");
        map2.put('%', "%2b");
        map2.put('\u00a1', "%C2%A1"); // inverted exclamation
        map2.put('\u00ED', "i"); // small i accute
        //map2.put('\u00F3', "%C3%B3"); // small o accute
        map2.put('\u00F3', "o"); // small o accute
        map2.put('\u00F1', "%C3%B1"); // small n with tilde
        map2.put('\u00FA', "%C3%BA"); // small u with acute
        map2.put('\u00E9', "%C3%A9"); // small e with acute
        map2.put('\u00E1', "%C3%A1"); // small a with acute
        map2.put('\u00FC', "%C3%BC"); // small Letter u with diaeresis
        map2.put('\u2018', "'"); // single left quotation
        //map.put('\u2018', "&#x2018;");
        map2.put('\u2019', "'"); // single right quotation
        //map.put('\u2019', "&#x2019;");
        map2.put('&', "&amp;");
        return map2;
    }

    public static String translate1(String in, HashMap<Character, String> map1)
    {
        StringBuffer out = new StringBuffer();

        for (int i = 0; i < in.length(); i++)
        {
            Character ch = in.charAt(i);
            String st = map1.get(in.charAt(i));
            if (st == null) out.append(in.charAt(i));
            else
                out.append(st);
        }
        return out.toString();
    }
    public static String translate2(String in, HashMap<Character, String> map2)
    {
        StringBuffer out = new StringBuffer();

        for (int i = 0; i < in.length(); i++)
        {
            Character ch = in.charAt(i);
            String st = map2.get(in.charAt(i));
            if (st == null) out.append(in.charAt(i));
            else
                out.append(st);
        }
        return out.toString();
    }
    
    
}
