import java.io.*;

public class FileOperator {

    public static String FILEPATH="D:\\workspace\\file\\";

//    public void separateFile(File file) {
//        if (file.exists()) {
//            long length = file.length();
//            int partNum = (int) (length / (1024 * 1024 * 10)) + 1;
//            String fileName = file.getName().substring(0, file.getName().indexOf("."));
//            InputStream inputStream = null;
//            try {
//                inputStream = new FileInputStream(file);
//                File dir = new File("D:\\workspace\\file\\" + fileName);
//                if (!dir.exists()) dir.mkdir();
//                for (int i = 0; i < partNum; i++) {
//                    StringBuffer sb = new StringBuffer();
//                    sb.append("D:\\workspace\\file\\");
//                    sb.append(fileName + "\\");
//                    sb.append(i + ".data");
//                    File childFile = new File(sb.toString());
//                    OutputStream outputStream = new FileOutputStream(childFile);
//                    int len;
//                    byte[] bys = new byte[1024 * 10];
//                    while ((len = inputStream.read(bys)) != -1) {
//                        outputStream.write(bys, 0, len);
//                        if (childFile.length() >= 1024 * 1024 * 10) break;
//                    }
//                    outputStream.close();
//                }
//                inputStream.close();
//            } catch (Exception t) {
//                System.out.println(t.getMessage());
//            }
//        }
//    }
//
//    public File mergeFile(String fileName) {
//        File path = new File("D:\\workspace\\file\\" + fileName + "\\");
//        File[] childFiles = path.listFiles();
//        File file = new File("D:\\workspace\\file\\" + fileName + "\\" + fileName + ".av");
//        try {
//            OutputStream outputStream = new FileOutputStream(file);
//            for (File childFile : childFiles) {
//                InputStream inputStream = new FileInputStream(childFile);
//                int len;
//                byte[] bys = new byte[10 * 1024];
//                while ((len = inputStream.read(bys)) != -1) {
//                    outputStream.write(bys, 0, len);
//                }
//                inputStream.close();
//            }
//            outputStream.close();
//        } catch (Exception e) {
//            System.out.println(e.getMessage());
//        }
//        return file;
//    }

    public File mergeByteToFile(byte[][] bs, String fileName, int fileLength) {
        File file = new File("D:\\" + fileName);
        try {
            OutputStream outputStream = new FileOutputStream(file);
            for (int i = 0; i < bs.length - 1; i++) {
                byte[] bytes = bs[i];
                outputStream.write(bytes, 0, bytes.length);
            }
            for (int i = 0; i < fileLength % (10 * 1024); i++) {
                byte b = bs[bs.length - 1][i];
                outputStream.write(b);
            }
            outputStream.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return file;
    }
}
