package gi.shotdatesort;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import gi.utils.FileWithDateCreate;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author andrey
 */
public class App {

    private static final int FIVE_MINUTE = 1000 * 60 * 5;
    private static final String NEW_DIRNAME = "TL_";
    private static final String PARAM_FILE_PATH = "-D=";
    private static final String PARAM_FILE_EXT = "-E=";
    private String filePath;
    private String fileExt;

    public App(String filePath, String fileExt) {
        this.filePath = filePath;
        this.fileExt = fileExt;
    }

    // получение даты снимка из EXIF
    private static Date getEXIFDATETIME_ORIGINAL(File file) {
        Metadata metadata;
        Date date = null;
        try {
            metadata = ImageMetadataReader.readMetadata(file);
            ExifSubIFDDirectory directory = metadata.getDirectory(ExifSubIFDDirectory.class);
            date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
        } catch (ImageProcessingException ex) {
            System.out.println(ex.getMessage());
        } catch (IOException ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        }

        // возврат null допустим
        return date;
    }

    // проверка директории на то, обрабатывалась ли она ранее скриптом, нет ли в ней лишних каталогов
    private boolean checkDir() {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(filePath))) {
            for (Path file : stream) {
                if (Files.isDirectory(file, LinkOption.NOFOLLOW_LINKS)) {
                    String nameDir = file.getName(file.getNameCount() - 1).toString();

                    if (nameDir.contains(NEW_DIRNAME)) {

                        Scanner in = new Scanner(System.in);

                        while (true) {
                            System.out.println("Directory " + nameDir + " is exists! Do you want to delete? (Y/N)");
                            String sIn = in.nextLine();
                            if (sIn.equalsIgnoreCase("Y")) {
                                deleteDirectory(file);
                                break;
                            } else if (sIn.equalsIgnoreCase("N")) {
                                return false;
                            }
                        }
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        return true;
    }

    // обработка указанной директории
    // создание списка объектов FileWithDateCreate
    private List<FileWithDateCreate> scanDirectory() throws ImageProcessingException {
        List<FileWithDateCreate> localList = new ArrayList();
        Path dir = Paths.get(filePath);
        int dirNum = 0;
        Date previousDate = new Date(0);

        DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept(Path file) throws IOException {
                String s = file.getFileName().toString();
                return s.toUpperCase().endsWith(fileExt);
            }
        };

        // фильтр по расширению файла
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, filter)) {

            for (Path file : stream) {
                // получение даты снимка из EXIF
                Date currentDate = getEXIFDATETIME_ORIGINAL(file.toFile());
                // если дата снимка не определена - пропускаем его
                if (currentDate == null) {
                    continue;
                }

                // если даты снимков отличаются более чем на 5 минут - 
                // далее скопируем их в разные каталоги
                if (Math.abs(currentDate.getTime() - previousDate.getTime()) > FIVE_MINUTE) {
                    dirNum++;
                }

                FileWithDateCreate m = new FileWithDateCreate();
                m.setDirNum(dirNum);
                m.setPath(file);
                m.setDateCreate(currentDate);
                localList.add(m);

                previousDate = currentDate;
            }
        } catch (IOException | DirectoryIteratorException x) {
            // IOException can never be thrown by the iteration.
            // In this snippet, it can only be thrown by newDirectoryStream.
            System.err.println(x);
        }

        return localList;
    }

    // рекурсивное удаление директории с содержимым
    private void deleteDirectory(Path file) throws IOException {
        if (Files.isDirectory(file, LinkOption.NOFOLLOW_LINKS)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(file)) {
                for (Path f : stream) {
                    deleteDirectory(f);
                }
            }
        }

        System.out.println("Delete" + file.toString());
        Files.deleteIfExists(file);
    }

    private void copyAndRename(List<FileWithDateCreate> listPaths) throws IOException {
        for (int i = 0; i < listPaths.size(); i++) {

            String newName = String.format("%06d", i) + fileExt;
            System.out.println("Rename" + listPaths.get(i).getPath().getFileName() + " to " + newName);

            Path oldPath = listPaths.get(i).getPath();
            Path newPath = Paths.get(filePath + "\\" + NEW_DIRNAME + String.format("%06d", listPaths.get(i).getDirNum()));

            // создадим директорию если она не существует 
            if (Files.notExists(newPath, new LinkOption[]{LinkOption.NOFOLLOW_LINKS})) {
                Files.createDirectory(newPath);
            }

            Files.copy(oldPath, newPath.resolve(newName));
        }
    }

    public void run() throws ImageProcessingException, IOException {

        // проверка директорию на что она была ранее обработана этим скриптом, 
        // удаление сгенерированных директорий и файлов
        if (!checkDir()) {
            System.out.println("checkDir() return False");
            return;
        }

        // сканирования директории и создание списка файлов
        List<FileWithDateCreate> listPaths = scanDirectory();

        // сортировка по dirNum затем по dateCreate
        Collections.sort(listPaths);

        // копирование в новые директории с переименованием        
        copyAndRename(listPaths);
    }

    public static void main(String[] args) {
        String sFilePath = "";
        String sFileExt = "";

        // sFilePath = "E:\\DCIM\\PHOTO\\TEST";
        // sFileExt = ".JPG";

        // обработаем параметры
        for (String s : args) {
            // путь к файлу
            if (s.toUpperCase().startsWith(PARAM_FILE_PATH)) {
                sFilePath = s.substring(PARAM_FILE_PATH.length());
                // уберем кавычки
                sFilePath = sFilePath.replace("\"", "");
                sFilePath = sFilePath.replace("\'", "");
            }

            if (s.toUpperCase().startsWith(PARAM_FILE_EXT)) {
                sFileExt = s.substring(PARAM_FILE_EXT.length());
            }
        }

        // параметры обязательны
        if (sFilePath.isEmpty() || sFileExt.isEmpty()) {
            System.out.println("Bad parameters!");
            return;
        }

        App app = new App(sFilePath, sFileExt);

        try {
            app.run();
        } catch (ImageProcessingException | IOException ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
