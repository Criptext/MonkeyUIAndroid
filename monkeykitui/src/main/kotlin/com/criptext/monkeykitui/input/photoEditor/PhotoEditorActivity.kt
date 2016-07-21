package com.criptext.monkeykitui.input.photoEditor

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.Button
import android.widget.ImageView
import com.criptext.monkeykitui.R
import com.criptext.monkeykitui.util.FlatButtonDrawable

import com.soundcloud.android.crop.Crop
import java.io.File


/**
 * Created by gesuwall on 9/8/15.
 */
class PhotoEditorActivity : AppCompatActivity() {

    private var photo: ImageView? = null
    private lateinit var retainedFragment: RetainEditPhoto
    private lateinit var tempFile : File
    lateinit var photoFilePath: String

    private var runningTask : PhotoEditTask? = null
    set(value) {
        if(field!=null)
            field?.cancel(true)
        field = value
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout)

        tempFile = File(cacheDir.absolutePath + "/tempEditedPhoto.png")
        retainFragment()

        photoFilePath = intent.getStringExtra(destinationPath)

        photo = findViewById(R.id.photo) as ImageView?

        //set Photo
        setEditingPhoto()

        //set button color
        setButtonColor()
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    private fun setButtonColor(){
        val sendButton = findViewById(R.id.sendPhoto) as Button
        val typedValue = TypedValue();
        val a = obtainStyledAttributes(typedValue.data, intArrayOf(R.attr.colorPrimary))
        val color = a.getColor(0,0)
        a.recycle()
        sendButton.background =FlatButtonDrawable.new(color)

    }

    /**

     * @return El codigo del layout para esta instancia de EditPhoto
     */
    protected val layout: Int
        get() = R.layout.activity_photo_editor

    /**
     * Coloca la imagen que se esta editando en el ImageView principal del activity. No recibe
     * parametros porque con los datos de retainedFragment se sabe de donde sacar la imagen
     * correspondiente.
     */
    private fun setEditingPhoto() {
        val imgpath = bitmapUri
        photo!!.setImageURI(null)
        photo!!.setImageURI(imgpath)
        //Rotar imagen de acuerdo al estado anterior
        if (retainedFragment.isRotated) {
            val rotated = getRotateDrawable(photo!!.drawable, retainedFragment.degrees.toFloat())
            photo!!.setImageDrawable(rotated)
        }

        //Fix photo height for rotation
        try {
            val input = contentResolver.openInputStream(imgpath)
            val onlyBoundsOptions = BitmapFactory.Options()
            onlyBoundsOptions.inJustDecodeBounds = true
            onlyBoundsOptions.inDither = true//optional
            onlyBoundsOptions.inPreferredConfig = Bitmap.Config.ARGB_8888//optional
            BitmapFactory.decodeStream(input, null, onlyBoundsOptions)
            input.close()
            if (onlyBoundsOptions.outHeight > onlyBoundsOptions.outWidth) {
                val scale = resources.displayMetrics.density
                val dpAsPixels = (45 * scale + 0.5f).toInt()
                photo!!.setPadding(dpAsPixels, dpAsPixels, dpAsPixels, dpAsPixels)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /**
     * Consigue la referencia al Fragment que se encarga de guarda los datos persistentes del Activity EditPhoto.
     * La convencion usada es siempre guardar el Fragment de Persistencia con el tag "data". Si el
     * Fragment ya existe lo saca del Fragment Manager para sacar los datos recuperados tras los
     * cambios de configuracion. De lo contrario, crea una nueva instancia del Fragment y lo inicializa
     * con los valores por defecto.
     */
    private fun retainFragment() {
        // find the retained fragment on activity restarts
        val fm = supportFragmentManager
        val fragment = fm.findFragmentByTag("data") as RetainEditPhoto?

        // create the fragment and data the first time
        if (fragment == null) {
            // add the fragment
            retainedFragment = RetainEditPhoto()
            fm.beginTransaction().add(retainedFragment, "data").commit()
            retainedFragment.isEdited = false
            retainedFragment.degrees = 0
            tempFile.delete() // delete temp file since this is the first time
        }
            else retainedFragment = fragment
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu items for use in the action bar
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_photo_editor, menu)
        return super.onCreateOptionsMenu(menu)
    }


    /**
     * Realiza la transformacion de rotacion en el archivo, y posteriormente manda el mismo archivo
     * al Activity que hace Crop.
     * @param origin Uri del archivo a rotar y recortar
     */
    fun cropAfterRotatingBitmapFile(origin: Uri){
        val rotationTask = PhotoEditTask(origin, tempFile.absolutePath, contentResolver)
        rotationTask.setOnBitmapProcessedCallback(Runnable {
                val destination = Uri.fromFile(tempFile)
                retainedFragment.isEdited = true
                retainedFragment.degrees = 0
                Crop.of(destination, destination).start(this@PhotoEditorActivity)
        })
        runningTask = rotationTask
        rotationTask.execute(editedDegrees)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.action_crop -> {

                val origin = bitmapUri
                val destination = Uri.fromFile(tempFile)
                if (retainedFragment.isRotated) {
                    // Si hay grados tengo que crear el bitmap rotado
                    cropAfterRotatingBitmapFile(origin)
                }else {
                    Crop.of(origin, destination).start(this)
                }
            }
            R.id.action_rot -> {
                val bitmapUri = bitmapUri
                val fromDegs = retainedFragment.degrees
                val toDegs = fromDegs - 90
                rotateImageView(fromDegs, toDegs)
                retainedFragment.degrees = toDegs
            }
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null && resultCode == Activity.RESULT_OK && requestCode == Crop.REQUEST_CROP) {
            retainedFragment.isEdited = true //Nueva foto asi que no hay rotacion
            rotateImageView(0, 0, 0)
            setEditingPhoto()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    public override fun onResume() {
        super.onResume()
    }

    /**
     * Crea un nuevo Drawable con rotacion
     * @param d Drawable original
     * *
     * @param angle grados a rotar
     * *
     * @return Drawable rotado
     */
    private fun getRotateDrawable(d: Drawable, angle: Float): Drawable {
        val arD = arrayOf(d)
        return object : LayerDrawable(arD) {
            override fun draw(canvas: Canvas) {
                canvas.save()
                canvas.rotate(angle, (d.bounds.width() / 2).toFloat(), (d.bounds.height() / 2).toFloat())
                super.draw(canvas)
                canvas.restore()
            }
        }
    }

    override fun onStop(){
        runningTask?.cancel(true)
        super.onStop()
    }

    /**
     * Inicia una animacion de rotacion sobre la foto a ser editada
     * @param fromDegrees angulo de inicio
     * *
     * @param toDegrees angulo de fin
     */
    private fun rotateImageView(fromDegrees: Int, toDegrees: Int, duration: Int = 400) {
        val rotate = RotateAnimation(fromDegrees.toFloat(), toDegrees.toFloat(), Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
        rotate.duration = duration.toLong()
        rotate.isFillEnabled = true
        rotate.fillAfter = true
        rotate.interpolator = LinearInterpolator()
        photo!!.startAnimation(rotate)

    }

    /**
     * Consigue el Uri del bitmap de acuerdo a si ha sido editado o no. Si ha sido editado hay que
     * sacarlo del archivo temporal, si no de la galeria
     * @return
     */
    protected val bitmapUri: Uri
        get() {
            if (retainedFragment.isEdited)
                return Uri.fromFile(tempFile)
            else
                return intent.data
        }

    /**
     * Callback de click del boton de Use/Send. Exporta la foto editada al archivo temporal y termina
     * el activity
     * @param view Boton que recibio el Click
     */
    fun done(view: View) {
        val processTask = PhotoEditTask(bitmapUri, photoFilePath, contentResolver)
        Log.d("PhotoEditor", "export source: ${bitmapUri.path} dest: $photoFilePath, degs: $editedDegrees")
        processTask.setOnBitmapProcessedCallback(Runnable {
            val intent = Intent()
            Log.d("PhotoEditor", "exported")
            setResult(Activity.RESULT_OK, intent)
            finish()
        })
        runningTask = processTask
        processTask.execute(editedDegrees)
    }

    protected val editedDegrees: Int
        get() = retainedFragment.degrees

    class RetainEditPhoto : Fragment() {

        // true si he editado la foto y debo de sacarla de la cache, de lo contrario tengo
        //que sacarla de la galeria
        var isEdited: Boolean = false
        var degrees: Int = 0

        // this method is only called once for this fragment
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            isEdited = false
            // retain this fragment
            retainInstance = true
        }

        val isRotated: Boolean
            get() = degrees % 360 != 0
    }

    companion object {
        val titlePhotoEditor = "PhotoEditorActivity.Title"
        val btnTxtPhotoEditor = "PhotoEditorActivity.ButtonText"
        val destinationPath = "PhotoEditorActivity.destinationPath"
    }
}
