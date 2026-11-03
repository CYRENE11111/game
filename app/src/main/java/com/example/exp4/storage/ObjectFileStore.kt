package com.example.exp4.storage

import android.content.Context
import android.util.Log
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

/**
 * 对象文件读写工具（实验要求的序列化存储方式）。
 *
 * 写文件：[Context.openFileOutput] + [ObjectOutputStream]；
 * 读文件：[Context.openFileInput] + [ObjectInputStream]。
 *
 * 写入对象必须实现 [Serializable]（或 Parcelable 等可序列化接口），
 * 文件保存在应用私有目录中，不需要申请任何存储权限。
 */
object ObjectFileStore {

    private const val TAG = "ObjectFileStore"

    /** 把对象序列化写入应用私有文件，失败时返回 false。 */
    fun save(context: Context, fileName: String, data: Serializable): Boolean {
        return try {
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { output ->
                ObjectOutputStream(output).use { it.writeObject(data) }
            }
            true
        } catch (e: IOException) {
            Log.e(TAG, "保存 $fileName 失败", e)
            false
        }
    }

    /** 从应用私有文件中反序列化读出对象；文件不存在或内容损坏时返回 null。 */
    @Suppress("UNCHECKED_CAST")
    fun <T : Serializable> load(context: Context, fileName: String): T? {
        return try {
            context.openFileInput(fileName).use { input ->
                ObjectInputStream(input).use { it.readObject() as? T }
            }
        } catch (e: IOException) {
            Log.w(TAG, "读取 $fileName 失败", e)
            null
        } catch (e: ClassNotFoundException) {
            Log.w(TAG, "读取 $fileName 时找不到对应的类", e)
            null
        }
    }
}
