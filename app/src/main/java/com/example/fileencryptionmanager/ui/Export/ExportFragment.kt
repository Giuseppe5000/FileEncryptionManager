package com.example.fileencryptionmanager.ui.Export;

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.database.CursorIndexOutOfBoundsException
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.fileencryptionmanager.FeedReaderDbHelper
import com.example.fileencryptionmanager.databinding.FragmentExportBinding
import com.example.prova.ui.notifications.ExportViewModel
import org.json.JSONArray
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class ExportFragment : Fragment() {

    private var json: JSONArray? = null

    private var _binding: FragmentExportBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val notificationsViewModel =
            ViewModelProvider(this).get(ExportViewModel::class.java)

        _binding = FragmentExportBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val exportButton: Button = binding.exportButton
        exportButton.setOnClickListener {

            // Get data from DB
            val dbHelper = context?.let { FeedReaderDbHelper(it) }

            // Gets the data repository in read mode
            val db = dbHelper?.readableDatabase

            val projection = arrayOf(
                FeedReaderDbHelper.FeedReaderContract.FeedEntry.MD5SUM,
                FeedReaderDbHelper.FeedReaderContract.FeedEntry.MIMETYPE,
                FeedReaderDbHelper.FeedReaderContract.FeedEntry.SALT,
                FeedReaderDbHelper.FeedReaderContract.FeedEntry.IV
            )

            val cursor = db?.query(
                FeedReaderDbHelper.FeedReaderContract.FeedEntry.TABLE_NAME,   // The table to query
                projection,             // The array of columns to return (pass null to get all)
                null,              // The columns for the WHERE clause
                null,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                null
            )

            // Get data
            try {
                cursor?.moveToFirst()

                val jsonString = StringBuilder("[")
                do {
                    val md5 = "\"${cursor?.getString(0)}\""
                    val mimetype = "\"${cursor?.getString(1)}\""
                    val salt = cursor?.getBlob(2).contentToString()
                    val iv = cursor?.getBlob(3).contentToString()

                    if (cursor?.isLast!!) {
                        jsonString.append("[$md5, $mimetype, $salt, $iv]")
                    } else {
                        jsonString.append("[$md5, $mimetype, $salt, $iv],")
                    }

                } while (cursor!!.moveToNext())

                jsonString.append("]")

                val jsonArray = JSONArray(jsonString.toString())
                json = jsonArray

                cursor.close()

                // Create database json file
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                    putExtra(Intent.EXTRA_TITLE, "database")

                    // Optionally, specify a URI for the directory that should be opened in
                    // the system file picker before your app creates the document.
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, "/")
                }
                startActivityForResult(intent, 1)

            } catch (e: CursorIndexOutOfBoundsException) {
                Toast.makeText(requireActivity(), "No data in DB", Toast.LENGTH_SHORT).show()
                cursor?.close()
            }

        }

        val importButton: Button = binding.importButton
        importButton.setOnClickListener {

            // Select json DB file
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"

                // Optionally, specify a URI for the file that should appear in the
                // system file picker when it loads.
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, "/")
            }
            startActivityForResult(intent, 2)


        }

        return root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {

        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            resultData?.data?.also { uri ->
                try {
                    requireActivity().contentResolver.openFileDescriptor(uri, "w")?.use {
                        FileOutputStream(it.fileDescriptor).use {
                            it.write(json.toString().toByteArray())
                            it.flush()
                            it.close()
                        }
                    }
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        } else if (requestCode == 2 && resultCode == Activity.RESULT_OK) {
            resultData?.data.also { uri ->
                var bytes: ByteArray? = null
                if (uri != null) {
                    requireActivity().contentResolver.openFileDescriptor(uri, "r").use { it ->
                        FileInputStream(
                            it?.fileDescriptor
                        ).use {
                            bytes = it.readBytes()
                            it.close()
                        }
                    }
                }

                //Read json data
                val json = JSONArray(String(bytes!!))
                for (i in 0 until json.length()) {

                    val jsonarray = json.getJSONArray(i)
                    val md5 = jsonarray.getString(0)
                    val mimetype = jsonarray.getString(1)

                    val salt = ByteArray(8)
                    for (j in 0 until jsonarray.getJSONArray(2).length()) {
                        salt[j] = jsonarray.getJSONArray(2).getInt(j).toByte()
                    }

                    val iv = ByteArray(16)
                    for (j in 0 until jsonarray.getJSONArray(3).length()) {
                        iv[j] = jsonarray.getJSONArray(3).getInt(j).toByte()
                    }


                    // SQLite conenct
                    val dbHelper = context?.let { FeedReaderDbHelper(it) }

                    // Gets the data repository in write mode
                    val db = dbHelper?.writableDatabase

                    val values = ContentValues().apply {
                        put(
                            FeedReaderDbHelper.FeedReaderContract.FeedEntry.MD5SUM,
                            md5
                        )
                        put(
                            FeedReaderDbHelper.FeedReaderContract.FeedEntry.MIMETYPE,
                            mimetype
                        )
                        put(
                            FeedReaderDbHelper.FeedReaderContract.FeedEntry.SALT,
                            salt
                        )
                        put(
                            FeedReaderDbHelper.FeedReaderContract.FeedEntry.IV,
                            iv
                        )
                    }

                    // Insert the new row, returning the primary key value of the new row
                    val newRowId = db?.insert(
                        FeedReaderDbHelper.FeedReaderContract.FeedEntry.TABLE_NAME,
                        null,
                        values
                    )

                }
            }

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}