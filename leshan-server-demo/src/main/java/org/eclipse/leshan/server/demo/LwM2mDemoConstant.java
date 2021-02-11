/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.demo;

public class LwM2mDemoConstant {
    // /!\ This field is a COPY of org.eclipse.leshan.client.demo.LeshanClientDemo.modelPaths /!\
    // TODO create a leshan-demo project ?
    public final static String[] modelPaths = new String[] { "8.xml", "9.xml", "10-1_0.xml", "10.xml", "11-1_0.xml",
                            "11.xml", "12-1_0.xml", "12.xml", "13-1_0.xml", "13.xml", "14.xml", "15.xml", "16.xml",
                            "19.xml", "20.xml", "22.xml", "500.xml", "501.xml", "502.xml", "503.xml", "2048.xml",
                            "2049.xml", "2050.xml", "2051.xml", "2052.xml", "2053.xml", "2054.xml", "2055.xml",
                            "2056.xml", "2057.xml", "3200-1_0.xml", "3200.xml", "3201-1_0.xml", "3201.xml",
                            "3202-1_0.xml", "3202.xml", "3203-1_0.xml", "3203.xml", "3300-1_0.xml", "3300.xml",
                            "3301-1_0.xml", "3301.xml", "3302-1_0.xml", "3302.xml", "3303-1_0.xml", "3303.xml",
                            "3304-1_0.xml", "3304.xml", "3305-1_0.xml", "3305.xml", "3306-1_0.xml", "3306.xml",
                            "3308-1_0.xml", "3308.xml", "3310-1_0.xml", "3310.xml", "3311.xml", "3312-1_0.xml",
                            "3312.xml", "3313-1_0.xml", "3313.xml", "3314-1_0.xml", "3314.xml", "3315-1_0.xml",
                            "3315.xml", "3316-1_0.xml", "3316.xml", "3317-1_0.xml", "3317.xml", "3318-1_0.xml",
                            "3318.xml", "3319-1_0.xml", "3319.xml", "3320-1_0.xml", "3320.xml", "3321-1_0.xml",
                            "3321.xml", "3322-1_0.xml", "3322.xml", "3323-1_0.xml", "3323.xml", "3324-1_0.xml",
                            "3324.xml", "3325-1_0.xml", "3325.xml", "3326-1_0.xml", "3326.xml", "3327-1_0.xml",
                            "3327.xml", "3328-1_0.xml", "3328.xml", "3329-1_0.xml", "3329.xml", "3330-1_0.xml",
                            "3330.xml", "3331-1_0.xml", "3331.xml", "3332-1_0.xml", "3332.xml", "3333-1_0.xml",
                            "3333.xml", "3334-1_0.xml", "3334.xml", "3335-1_0.xml", "3335.xml", "3336-1_0.xml",
                            "3336.xml", "3337-1_0.xml", "3337.xml", "3338-1_0.xml", "3338.xml", "3339.xml", "3340.xml",
                            "3341.xml", "3342-1_0.xml", "3342.xml", "3343.xml", "3344.xml", "3345.xml", "3346-1_0.xml",
                            "3346.xml", "3347-1_0.xml", "3347.xml", "3348-1_0.xml", "3348.xml", "3349-1_0.xml",
                            "3349.xml", "3350-1_0.xml", "3350.xml", "3351.xml", "3352.xml", "3353.xml", "3354.xml",
                            "3355.xml", "3356.xml", "3357.xml", "3358.xml", "3359.xml", "3360.xml", "3361.xml",
                            "3362.xml", "3363.xml", "3364.xml", "3365.xml", "3366.xml", "3367.xml", "3368.xml",
                            "3369.xml", "3370.xml", "3371.xml", "3372.xml", "3373.xml", "3374.xml", "3375.xml",
                            "3376.xml", "3377.xml", "3378.xml", "3379.xml", "3380-1_0.xml", "3380.xml", "3381.xml",
                            "3382.xml", "3383.xml", "3384.xml", "3385.xml", "3386.xml", "3387.xml", "3388.xml",
                            "3389.xml", "3390.xml", "3391.xml", "3392.xml", "3393.xml", "3394.xml", "3395.xml",
                            "3396.xml", "3397.xml", "3398.xml", "3399.xml", "3400.xml", "3401.xml", "3402.xml",
                            "3403.xml", "3404.xml", "3405.xml", "3406.xml", "3407.xml", "3408.xml", "3410.xml",
                            "3411.xml", "3412.xml", "3413.xml", "3414.xml", "3415.xml", "3416.xml", "3417.xml",
                            "3418.xml", "3419.xml", "3420.xml", "3421.xml", "3423.xml", "3424.xml", "3425.xml",
                            "3426.xml", "3427.xml", "3428.xml", "3429.xml", "3430.xml", "3431.xml", "3432.xml",
                            "3433.xml", "3434.xml", "3435.xml", "3436.xml", "3437.xml", "3438.xml", "3439.xml",
                            "10241.xml", "10242.xml", "10243.xml", "10244.xml", "10245.xml", "10246.xml", "10247.xml",
                            "10248.xml", "10249.xml", "10250.xml", "10251.xml", "10252.xml", "10253.xml", "10254.xml",
                            "10255.xml", "10256.xml", "10257.xml", "10258.xml", "10259.xml", "10260-1_0.xml",
                            "10260.xml", "10262.xml", "10263.xml", "10264.xml", "10265.xml", "10266.xml", "10267.xml",
                            "10268.xml", "10269.xml", "10270.xml", "10271.xml", "10272.xml", "10273.xml", "10274.xml",
                            "10275.xml", "10276.xml", "10277.xml", "10278.xml", "10279.xml", "10280.xml", "10281.xml",
                            "10282.xml", "10283.xml", "10284.xml", "10286.xml", "10290.xml", "10291.xml", "10292.xml",
                            "10299.xml", "10300.xml", "10308-1_0.xml", "10308.xml", "10309.xml", "10311-1_0.xml",
                            "10311.xml", "10313.xml", "10314.xml", "10315.xml", "10316.xml", "10318.xml", "10319.xml",
                            "10320.xml", "10322.xml", "10323.xml", "10324.xml", "10326.xml", "10327.xml", "10328.xml",
                            "10329.xml", "10330.xml", "10331.xml", "10332.xml", "10333.xml", "10334.xml", "10335.xml",
                            "10336.xml", "10337.xml", "10338.xml", "10339.xml", "10340.xml", "10341.xml", "10342.xml",
                            "10343.xml", "10344.xml", "10345.xml", "10346.xml", "10347.xml", "10348.xml", "10349.xml",
                            "10350.xml", "10351.xml", "10352.xml", "10353.xml", "10354.xml", "10355.xml", "10356.xml",
                            "10357.xml", "10358.xml", "10359.xml", "10360.xml", "10361.xml", "10362.xml", "10363.xml",
                            "10364.xml", "10365.xml", "10366.xml", "10368.xml", "10369.xml", "10371.xml", "18830.xml",
                            "18831.xml", };
}
