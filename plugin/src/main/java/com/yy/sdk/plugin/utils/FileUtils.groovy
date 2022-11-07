package com.yy.sdk.plugin.utils;

/**
 * Created by nls on 2020/8/26.
 * Nothing.
 */
class FileUtils {

    public static boolean hasExtension(File file, String extension) {
        return file.getPath().endsWith(extension)
    }

    public static void deleteFile(String path) {
        File file = new File(path)
        deleteFile(file)
    }

    public static void deleteFile(File file) {
        if (!file.exists()) {
            return
        }
        if (file.isFile()) {
            file.delete()
        } else if (file.isDirectory()) {
            file.deleteDir()
        }
    }

    public static void deleteDir(String path) {
        File pathFile = new File(path)
        if (!pathFile.exists()) {
            return
        }
        File[] files = pathFile.listFiles()
        for (int i = 0; i < files.length; i++) {
            deleteFile(files[i])
        }
    }

    public static void collectionFilesFromDir(
            Collection<String> files, String dir, String suffix) {
        collectionFilesFromDir(files, new File(dir), suffix)
    }

    public static void collectionFilesFromDir(
            Collection<String> files, File dir, String suffix) {
        if (!dir.exists()) {
            return
        }
        File[] fs = dir.listFiles()
        for (File f : fs) {
            if (f.isDirectory()) {
                collectionFiles(files, f)
            } else if (f.isFile() && f.path.endsWith(suffix)) {
                files.addAll(f.absolutePath)
            }
        }
    }

    public static String readFile(String filePath) {
        return readFile(new File(filePath))
    }

    public static String readFile(File file) {
        if (file.exists() && file.isFile() && file.canRead()) {
            FileInputStream fileInputStream = new FileInputStream(file);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            StringBuffer sb = new StringBuffer();
            String text = null;
            while ((text = bufferedReader.readLine()) != null) {
                sb.append(text);
                sb.append(System.lineSeparator())
            }
            bufferedReader.close()
            return sb.toString();
        } else {
            return ""
        }
    }

    public static boolean writeFile(File file, String str) {
        if (!file.exists()) {
            file.createNewFile()
        }
        if (!file.canWrite()) {
            file.delete()
            file.createNewFile()
        }
        FileOutputStream fileOutputStream = new FileOutputStream(file)
        fileOutputStream.write(str.getBytes())
        fileOutputStream.flush()
        fileOutputStream.close()
        return true
    }
}
