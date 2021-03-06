/**
* @file		: AtoiReturnValueAsArrayIndex.cpp
* @brief	: AtoiReturnValueAsArrayIndex class source file
*			
* @author	: 
* @date		: 
*
* Copyright 2015 by Samsung Electronics Inc.
*
* This software is the confidential and proprietary information
* of Samsung Electronics Inc. (Confidential Information).  You
* shall not disclose such Confidential Information and shall use
* it only in accordance with the terms of the license agreement
* you entered into with Samsung.
*
*/

void TestMethod11()
{
    char str[]="-1";
	char str2[]="1"; 
	char array[256]={0,};
	
	//this checker should detect for below code	
	array[atoi(str)] = 0; //atoi(str) returns negative value: -1 	
	
	//this checker should not detect for below code
	array[atoi(str2)]=0; //atoi(str2) returns positive value: 1
	
	//this checker should detect for below code
	int i=atoi(str);
	array[i] = 0; //i is negative value: -1, and i is used as index of array without checking the range of i 	
	
	//this checker should not detect for below code 
	int j=atoi(str2);	
	array[j] = 0;

}

