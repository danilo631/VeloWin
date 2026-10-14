package dev.velowin.app.runtime;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

/** Grants the Android package installer read-only access to ONE verified APK. */
public final class ComponentApkProvider extends ContentProvider {
    private static final String AUTH="dev.velowin.app.componentapk";
    @Override public boolean onCreate(){return true;}
    private ComponentRelease release(Uri uri)throws FileNotFoundException {
        if(uri==null||!AUTH.equals(uri.getAuthority())
            ||!"/termux-x11".equals(uri.getPath()))
            throw new FileNotFoundException("Componente não autorizado.");
        ComponentRelease entry=ComponentRegistry.get(getContext(),
            ComponentRelease.Kind.TERMUX_X11_APK);
        if(entry==null)throw new FileNotFoundException("Pacote não verificado.");
        return entry;
    }
    @Override public String getType(Uri uri) {
        return AUTH.equals(uri.getAuthority()) && "/termux-x11".equals(uri.getPath())
            ?"application/vnd.android.package-archive":null;
    }
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode)
            throws FileNotFoundException {
        if(!"r".equals(mode))throw new FileNotFoundException("Somente leitura.");
        ComponentRelease entry=release(uri);
        try {
            if(!ComponentStore.verifyApk(getContext(),entry))
                throw new FileNotFoundException("SHA-256 inválido.");
            File apk=ComponentStore.apk(getContext(),entry);
            if(!apk.isFile()||apk.length()!=entry.size)
                throw new FileNotFoundException("Pacote indisponível.");
            return ParcelFileDescriptor.open(apk,ParcelFileDescriptor.MODE_READ_ONLY);
        }catch(FileNotFoundException fail){throw fail;}
        catch(Exception error){throw new FileNotFoundException(error.getMessage());}
    }
    @Override public Cursor query(Uri uri,String[] projection,String selection,
                    String[] selectionArgs,String sortOrder){
        try{
            ComponentRelease entry=release(uri);
            String[] columns=projection==null
                ?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;
            MatrixCursor cursor=new MatrixCursor(columns);
            Object[] row=new Object[columns.length];
            for(int i=0;i<columns.length;i++){
                if(OpenableColumns.DISPLAY_NAME.equals(columns[i]))row[i]=entry.name;
                if(OpenableColumns.SIZE.equals(columns[i]))row[i]=entry.size;
            }
            cursor.addRow(row);
            return cursor;
        }catch(Exception error){return null;}
    }
    @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
    @Override public int delete(Uri uri,String selection,String[] selectionArgs){
        throw new UnsupportedOperationException();
    }
    @Override public int update(Uri uri,ContentValues values,String selection,String[] args){
        throw new UnsupportedOperationException();
    }
}
