package de.kaffeemitkoffein.tinyweatherforecastgermany;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.*;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ForecastBitmap{

    private Context context;
    private ArrayList<Weather.WeatherInfo> weatherInfos;
    private Weather.WeatherLocation weatherLocation;
    private int bitmapWidth = 0;
    private int bitmapHeight = 0;
    private int anticipatedWidth = 0;
    private float itemWidth;
    private float iconRatio;
    private float iconHeight;
    private float fontSize_small;
    private float fontSize_medium;
    private boolean displayWind = false;

    static class Builder{
        private ArrayList<Weather.WeatherInfo> weatherInfos;
        private Weather.WeatherLocation weatherLocation;
        private int bitmapWidth=0;
        private int bitmapHeight=0;
        private int anticipatedWidth =0;
        private float iconRatio = (float) 0.5;
        private boolean displayWind = false;

        public Builder setWetherInfos(ArrayList<Weather.WeatherInfo> weatherInfos){
            this.weatherInfos = weatherInfos;
            return this;
        }

        public Builder setWeatherLocation(Weather.WeatherLocation weatherLocation){
            this.weatherLocation = weatherLocation;
            return this;
        }

        public Builder setAnticipatedWidth(int i){
            this.anticipatedWidth = i;
            return this;
        }

        public Builder setWidth(int width){
            this.bitmapWidth = width;
            return this;
        }

        public Builder setHeight(int height){
            this.bitmapHeight = height;
            return this;
        }

        public Builder setIconRatio(float f){
            this.iconRatio = f;
            return this;
        }

        public Builder displayWind(boolean b){
            this.displayWind = b;
            return this;
        }

        public ForecastBitmap create(Context context){
            return new ForecastBitmap(context,this);
        }
    }

    private ForecastBitmap(Context context, final Builder builder){
        this.context = context;
        this.weatherInfos =  builder.weatherInfos;
        this.weatherLocation = builder.weatherLocation;
        this.anticipatedWidth = builder.anticipatedWidth;
        this.bitmapWidth = builder.bitmapWidth;
        this.bitmapHeight = builder.bitmapHeight;
        this.iconRatio = builder.iconRatio;
        this.displayWind = builder.displayWind;
        if ((bitmapHeight==0) || (bitmapWidth==0)){
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (windowManager!=null){
                DisplayMetrics displayMetrics = new DisplayMetrics();
                windowManager.getDefaultDisplay().getMetrics(displayMetrics);
                float screenAspectRatio = getPhysicalDisplayRatio(context);
                float screenWidth  = displayMetrics.widthPixels;
                float screenHeight = displayMetrics.heightPixels;
                if (screenWidth>screenHeight){
                    // landscape mode
                    bitmapWidth  = (int) screenWidth;
                    //bitmapHeight = (int) ((((bitmapWidth/displayMetrics.xdpi) * (displayMetrics.ydpi)) / 20) * screenAspectRatio);
                    bitmapHeight = (int) (((bitmapWidth/displayMetrics.xdpi) * (displayMetrics.ydpi)) / 20);
                } else {
                    // portrait mode
                    bitmapWidth = displayMetrics.widthPixels;
                    bitmapHeight = (displayMetrics.heightPixels / 20);
                }
            }
        }
    }

    private Bitmap getIconBitmap(Context context, Weather.WeatherInfo weatherInfo, int bitmapWidth, int bitmapHeight){
        // set default resource to not available;
        int resource = R.mipmap.not_available;
        if (weatherInfo.hasCondition()){
            // display always daytime
            resource = WeatherCodeContract.getWeatherConditionDrawableResource(weatherInfo.getCondition(),true);
            // calculate daytime precisely if location is set
            if (weatherLocation!=null){
                resource = WeatherCodeContract.getWeatherConditionDrawableResource(weatherInfo.getCondition(), weatherInfo.isDaytime(this.weatherLocation));
            }
        }
        Bitmap bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(context.getResources(),resource),
                bitmapWidth,
                bitmapHeight,
                true);
        return bitmap;
    }

    private void drawPolygon(Canvas canvas, float[] poly_x, float[] poly_y, int color, int alpha){
        Paint poly_paint = new Paint();
        poly_paint.setColor(color);
        poly_paint.setAlpha(alpha);
        poly_paint.setStyle(Paint.Style.FILL_AND_STROKE);
        Path path = new Path();
        path.moveTo(0,poly_y[0]);
        for (int i=0; i<poly_x.length; i++){
            path.lineTo(poly_x[i],poly_y[i]);
        }
        canvas.drawPath(path,poly_paint);
    }

    public Bitmap getForecastBitmap(){
        if (weatherInfos==null){
            return null;
        }
        if (weatherInfos.size()==0){
            return null;
        }
        if ((bitmapWidth<=0)||(bitmapHeight<=0)){
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(bitmapWidth,bitmapHeight,Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Log.v("TWFG","Width: "+bitmapWidth);
        Log.v("TWFG","Hight: "+bitmapHeight);
        itemWidth = (float) (bitmapWidth / anticipatedWidth);
        Log.v("TWFG","Item-Width: "+itemWidth);
        fontSize_medium  = (float) (bitmapHeight/2.2);
        fontSize_small   = (float) (bitmapHeight/3.3);
        Paint paint = new Paint();
        paint.setColor(getColorFromResource(R.color.textColor));
        paint.setTextSize(fontSize_medium);
        float x_offset = (bitmapWidth - itemWidth);
        // draw polygons for rain and clouds
        float[] x_polygon = new float[weatherInfos.size()+4];
        float[] y_polygon_rain = new float[weatherInfos.size()+4];
        float[] y_polygon_clouds = new float[weatherInfos.size()+4];
        // calculate offset of polygons when less than 6 items
        float polygon_x_offset = (6-weatherInfos.size())*itemWidth;
        // calculate values of polygons
        for (int j=0; j<weatherInfos.size(); j++){
            x_polygon[j] = polygon_x_offset + itemWidth*j + itemWidth/2;
            Weather.WeatherInfo wi = weatherInfos.get(j);
            if (wi.hasProbPrecipitation()){
                float pp = (float) wi.getProbPrecipitation()/100;
                y_polygon_rain[j] = (bitmapHeight - pp*bitmapHeight);
            } else {
                y_polygon_rain[j] = bitmapHeight;
            }
            if (wi.hasClouds()){
                float pc = (float) wi.getClouds()/100;
                y_polygon_clouds[j] = (bitmapHeight - pc*bitmapHeight);
            } else {
                y_polygon_clouds[j] = bitmapHeight;
            }
            x_polygon[weatherInfos.size()] = bitmapWidth;
            y_polygon_clouds[weatherInfos.size()] = y_polygon_clouds[weatherInfos.size()-1];
            x_polygon[weatherInfos.size()+1] = bitmapWidth;
            y_polygon_clouds[weatherInfos.size()+1] = bitmapHeight;
            x_polygon[weatherInfos.size()+2] = polygon_x_offset;
            y_polygon_clouds[weatherInfos.size()+2] = bitmapHeight;
            x_polygon[weatherInfos.size()+3] = polygon_x_offset;
            y_polygon_clouds[weatherInfos.size()+3] = y_polygon_clouds[0];

            y_polygon_rain[weatherInfos.size()] = y_polygon_rain[weatherInfos.size()-1];
            y_polygon_rain[weatherInfos.size()+1] = bitmapHeight;
            y_polygon_rain[weatherInfos.size()+2] = bitmapHeight;
            y_polygon_rain[weatherInfos.size()+3] = y_polygon_rain[0];

        }
        drawPolygon(canvas,x_polygon,y_polygon_clouds,getColorFromResource(R.color.fcitem_clouds),65);
        drawPolygon(canvas,x_polygon,y_polygon_rain,getColorFromResource(R.color.fcitem_rain),85);
        int position = weatherInfos.size()-1;
        while (position>=0){
            // draw timestamp
            SimpleDateFormat format = new SimpleDateFormat("HH:mm");
            Date date = new Date();
            date.setTime(weatherInfos.get(position).getTimestamp());
            String timetext = format.format(date);
            paint.setTextSize(fontSize_small);
            paint.setAlpha(255);
            canvas.drawText(timetext,x_offset,fontSize_small,paint);
            // draw icon
            Weather.WeatherInfo wi = weatherInfos.get(position);
            float iconsize = itemWidth*iconRatio;
            if (iconsize>bitmapHeight-fontSize_small-1){
                iconsize = bitmapHeight-fontSize_small-1;
            }
            Log.v("TWFG","Icon size: "+iconsize);
            canvas.drawBitmap(getIconBitmap(context, wi,Math.round(iconsize),Math.round(iconsize)),x_offset, fontSize_small+1,paint);
            // place temperature
            String temperature_text = weatherInfos.get(position).getTemperatureInCelsiusInt()+"°";
            paint.setTextSize(fontSize_medium);
            // canvas.drawText(temperature_text,x_offset+itemWidth*iconRatio,(float) (bitmapHeight/2)+fontSize_medium/2,paint);
            canvas.drawText(temperature_text,x_offset+iconsize+1,(float) (bitmapHeight/2)+fontSize_medium/2,paint);
            // place further temperature information if space is available
            float x_within_item_offset = paint.measureText(temperature_text)+iconsize+2;
            if ((itemWidth-x_within_item_offset)>=fontSize_small*3){
                Log.v("TWFG","Yes, we have further space.");
                if (weatherInfos.get(position).hasMaxTemperature() || weatherInfos.get(position).hasMinTemperature()){
                    Paint paint_minmax = new Paint();
                    paint_minmax.setColor(getColorFromResource(R.color.textColor));
                    paint_minmax.setAlpha(255);
                    paint_minmax.setTextSize(fontSize_small);
                    String temperature_max ="";
                    String temperature_min ="";
                    float max_text_width = 0;
                    if (weatherInfos.get(position).hasMaxTemperature()){
                        temperature_max = weatherInfos.get(position).getMaxTemperatureInCelsiusInt()+"°";
                        max_text_width = paint_minmax.measureText(temperature_max);
                    }
                    if (weatherInfos.get(position).hasMinTemperature()){
                        temperature_min = weatherInfos.get(position).getMinTemperatureInCelsiusInt()+"°";
                        float width_mintemp = paint_minmax.measureText(temperature_min);
                        if (width_mintemp>max_text_width){
                            max_text_width = width_mintemp;
                        }
                    }
                    // float minmax_width = paint_minmax.measureText()
                    // float x_minmax = x_offset + paint.measureText(temperature_text)+iconsize+2;
                    float y_max    = (bitmapHeight - (paint_minmax.getTextSize()*2)-2)/2 + paint_minmax.getTextSize();
                    float y_min    = (bitmapHeight - (paint_minmax.getTextSize()*2)-2)/2 + paint_minmax.getTextSize()*2+1;
                    if (weatherInfos.get(position).hasMaxTemperature()){
                        float x_max = x_offset + x_within_item_offset + (max_text_width-paint_minmax.measureText(temperature_max))/2;
                        canvas.drawText(temperature_max,x_max,y_max,paint_minmax);
                    }
                    if (weatherInfos.get(position).hasMinTemperature()){
                        float x_min = x_offset + x_within_item_offset + (max_text_width-paint_minmax.measureText(temperature_min))/2;
                        canvas.drawText(temperature_min,x_min,y_min,paint_minmax);
                    }
                    x_within_item_offset = x_within_item_offset + max_text_width + 1;
                }
            }
            if (itemWidth - x_within_item_offset>=iconsize*(float) 0.8 + 1){
                Log.v("TWFG","Yes, we have even more further space.");
                if (weatherInfos.get(position).hasWindSpeed()||weatherInfos.get(position).hasWindDirection()){
                    WeatherSettings weatherSettings = new WeatherSettings(context);
                    Bitmap windsymbol = weatherInfos.get(position).getWindSymbol(context);
                    windsymbol = Bitmap.createScaledBitmap(windsymbol,Math.round(iconsize*(float) 0.8),Math.round(iconsize*(float) 0.8),false);
                    float y_offset_wind = (bitmapHeight - windsymbol.getHeight())/2;
                    canvas.drawBitmap(windsymbol,x_offset + x_within_item_offset,y_offset_wind,null);
                    x_within_item_offset = x_within_item_offset + iconsize*(float) 0.8 + 1;
                }
            }
            if (itemWidth - x_within_item_offset>=fontSize_small*3){
                Log.v("TWFG","Yes, we have even more further, further ++ space.");
                if (weatherInfos.get(position).hasWindSpeed()){
                    String windspeedstring = weatherInfos.get(position).getWindSpeedString(context,true);
                    Paint windspeed_paint = new Paint();
                    windspeed_paint.setColor(MainActivity.getColorFromResource(context,R.color.widget_textcolor));
                    windspeed_paint.setTextSize(fontSize_small);
                    float y_offset = (bitmapHeight - fontSize_small)/2+fontSize_small;
                    canvas.drawText(windspeedstring,x_offset+x_within_item_offset,y_offset,windspeed_paint);
                    x_within_item_offset = x_within_item_offset + fontSize_small*3;
                }
            }
            x_offset = x_offset - itemWidth;
            position--;
        }
        Log.v("TWFG","final size: "+bitmap.getWidth()+"/"+bitmap.getHeight());
        return bitmap;
    }

    @SuppressWarnings("deprecation")
    public int getColorFromResource(int id){
        int color;
        if (Build.VERSION.SDK_INT<23){
            return context.getResources().getColor(id);
        } else {
            return context.getResources().getColor(id, context.getTheme());
        }
    }

    /**
     * Determines the display ratio of the screen based on the screen resolution in pixels. Pixels may
     * have different denities in the x- and y-axis. Therefore use getPhysicalDisplayRatio to determine
     * the real (physical) screen ratio based on the screen size in inches.
     *
     * @param context the current context
     * @return the screen ratio, or 0 if the display metrics are not available.
     */

    public static float getPixelDisplayRatio(Context context){
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager!=null){
            DisplayMetrics displayMetrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
            float widthPixels  = displayMetrics.widthPixels;
            float heightPixels = displayMetrics.heightPixels;
            if ((heightPixels!=0) && (widthPixels!=0)){
                if (heightPixels>widthPixels){
                    return heightPixels/widthPixels;
                } else {
                    return widthPixels/heightPixels;
                }
            }
        }
        return 0;
    }

    /**
     * Determines the display ratio based on the physical screen size in inches. This may give a different
     * result as calculating the display ratio from pixels, since pixel densities may be different for the
     * x- and the y- axis.
     *
     * @param context the current context
     * @return display ratio, or 0 if the display metrics are not available.
     */

    public static float getPhysicalDisplayRatio(Context context){
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager!=null){
            DisplayMetrics displayMetrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
            if ((displayMetrics.xdpi!=0) && (displayMetrics.ydpi!=0)){
                float widthInch  = displayMetrics.widthPixels / displayMetrics.xdpi;
                float heightInch = displayMetrics.heightPixels / displayMetrics.ydpi;
                if ((heightInch!=0) && (widthInch!=0)){
                    if (heightInch>widthInch){
                        return heightInch/widthInch;
                    } else {
                        return widthInch/heightInch;
                    }
                }
            }
        }
        return 0;
    }

    public static int getDisplayOrientation(Context context){
        return context.getResources().getConfiguration().orientation;
    }

}
