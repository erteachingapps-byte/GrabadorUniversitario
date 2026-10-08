package com.edwin.grabador

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.Button

object UiTheme {
 fun dark(context:Context)=context.getSharedPreferences("appearance",Context.MODE_PRIVATE).getBoolean("dark",false)
 fun background(dark:Boolean)=if(dark) Color.rgb(17,23,34) else Color.rgb(250,251,253)
 fun foreground(dark:Boolean)=if(dark) Color.WHITE else Color.rgb(28,35,48)
 fun apply(activity:Activity){activity.window.statusBarColor=background(dark(activity));activity.window.navigationBarColor=background(dark(activity));activity.window.decorView.systemUiVisibility=if(dark(activity)) 0 else android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR}
 fun color(name:String):Int=when {
  name.startsWith("Lunes")->0xFF4467C4.toInt()
  name.startsWith("Martes 1")->0xFF008C95.toInt()
  name.startsWith("Martes 2")->0xFF7B55BA.toInt()
  name.startsWith("Miércoles 1")->0xFFB06A25.toInt()
  name.startsWith("Miércoles 2")->0xFFB44783.toInt()
  name.startsWith("Jueves")->0xFF39804B.toInt()
  name.startsWith("Viernes")->0xFFB4494E.toInt()
  else->0xFF55647C.toInt()
 }
 fun style(button:Button,color:Int){button.setTextColor(Color.WHITE);button.background=GradientDrawable().apply{setColor(color);cornerRadius=18f}}
}
