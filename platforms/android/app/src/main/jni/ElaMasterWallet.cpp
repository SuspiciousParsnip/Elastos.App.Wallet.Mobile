// Copyright (c) 2012-2018 The Elastos Open Source Project
// Distributed under the MIT software license, see the accompanying
// file COPYING or http://www.opensource.org/licenses/mit-license.php.

#include "ElaUtils.h"
#include "IMasterWallet.h"
#include "nlohmann/json.hpp"

using namespace Elastos::SDK;

//"(JILjava/lang/String;ILjava/lang/String;ZJ)J"
static jlong JNICALL nativeCreateSubWallet(JNIEnv *env, jobject clazz, jlong jMasterProxy, jint jType, jstring jChainID, jint jCoinTypeIndex,
        jstring jpayPassword, jboolean jSingleAddress, jlong jFeePerKb)
{
    const char* chainID = env->GetStringUTFChars(jChainID, NULL);
    const char* payPassword = env->GetStringUTFChars(jpayPassword, NULL);

    IMasterWallet* masterWallet = (IMasterWallet*)jMasterProxy;
    ISubWallet* subWallet = masterWallet->CreateSubWallet((SubWalletType)jType, chainID, jCoinTypeIndex, payPassword, jSingleAddress, jFeePerKb);

    env->ReleaseStringUTFChars(jChainID, chainID);
    env->ReleaseStringUTFChars(jpayPassword, payPassword);
    return (jlong)subWallet;
}

//"(JILjava/lang/String;ILjava/lang/String;ZIJ)J"
static jlong JNICALL nativeRecoverSubWallet(JNIEnv *env, jobject clazz, jlong jMasterProxy, jint jType, jstring jChainID, jint jCoinTypeIndex,
        jstring jpayPassword, jboolean jSingleAddress, jint limitGap, jlong jFeePerKb)
{
    const char* chainID = env->GetStringUTFChars(jChainID, NULL);
    const char* payPassword = env->GetStringUTFChars(jpayPassword, NULL);

    IMasterWallet* masterWallet = (IMasterWallet*)jMasterProxy;
    ISubWallet* subWallet = masterWallet->RecoverSubWallet((SubWalletType)jType, chainID, jCoinTypeIndex, payPassword, jSingleAddress, limitGap, jFeePerKb);

    env->ReleaseStringUTFChars(jChainID, chainID);
    env->ReleaseStringUTFChars(jpayPassword, payPassword);
    return (jlong)subWallet;
}

//"(JJ)V"
static void JNICALL nativeDestroyWallet(JNIEnv *env, jobject clazz, jlong jMasterProxy, jlong jsubWalletProxy)
{
    IMasterWallet* masterWallet = (IMasterWallet*)jMasterProxy;
    ISubWallet* subWallet = (ISubWallet*)jsubWalletProxy;
    masterWallet->DestroyWallet(subWallet);
}

//"(J)Ljava/lang/String;"
static jstring JNICALL nativeGetPublicKey(JNIEnv *env, jobject clazz, jlong jMasterProxy)
{
    IMasterWallet* masterWallet = (IMasterWallet*)jMasterProxy;
    std::string key = masterWallet->GetPublicKey();
    return env->NewStringUTF(key.c_str());
}

//"(JLjava/lang/String;Ljava/lang/String;)Ljava/lang/String;"
static jstring JNICALL nativeSign(JNIEnv *env, jobject clazz, jlong jMasterProxy, jstring jmessage, jstring jpayPassword)
{
    const char* message = env->GetStringUTFChars(jmessage, NULL);
    const char* payPassword = env->GetStringUTFChars(jpayPassword, NULL);

    IMasterWallet* masterWallet = (IMasterWallet*)jMasterProxy;
    std::string result = masterWallet->Sign(message, payPassword);

    env->ReleaseStringUTFChars(jmessage, message);
    env->ReleaseStringUTFChars(jpayPassword, payPassword);
    return env->NewStringUTF(result.c_str());
}

//"(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"
static /*nlohmann::json*/jstring JNICALL nativeCheckSign(JNIEnv *env, jobject clazz, jlong jMasterProxy, jstring jaddress, jstring jmessage,
        jstring jsignature)
{
    const char* address = env->GetStringUTFChars(jaddress, NULL);
    const char* message = env->GetStringUTFChars(jmessage, NULL);
    const char* signature = env->GetStringUTFChars(jsignature, NULL);

    IMasterWallet* masterWallet = (IMasterWallet*)jMasterProxy;
    nlohmann::json jsonVal = masterWallet->CheckSign(address, message, signature);
    std::string result = jsonVal;

    env->ReleaseStringUTFChars(jaddress, address);
    env->ReleaseStringUTFChars(jmessage, message);
    env->ReleaseStringUTFChars(jsignature, signature);

    return env->NewStringUTF(result.c_str());
}

//"(JIILjava/lang/String;Ljava/lang/Object;)Z"
static jboolean JNICALL nativeDeriveIdAndKeyForPurpose(JNIEnv *env, jobject clazz, jlong jMasterProxy,
        jint purpose, jint index, jstring jpayPassword, jobject jIdKeyObj)
{
    const char* payPassword = env->GetStringUTFChars(jpayPassword, NULL);

    std::string key;
    std::string id;
    IMasterWallet* masterWallet = (IMasterWallet*)jMasterProxy;
    bool status = masterWallet->DeriveIdAndKeyForPurpose(purpose, index, payPassword, id, key);
    jclass idKeyKlass = env->FindClass("com/elastos/spvcore/IMasterWallet$IDKEY");
    jfieldID idField = env->GetFieldID(idKeyKlass, "id", "Ljava/lang/String;");
    jfieldID keyField = env->GetFieldID(idKeyKlass, "key", "Ljava/lang/String;");
    env->SetObjectField(jIdKeyObj, idField, env->NewStringUTF(id.c_str()));
    env->SetObjectField(jIdKeyObj, keyField, env->NewStringUTF(key.c_str()));

    env->ReleaseStringUTFChars(jpayPassword, payPassword);
    return (jboolean)status;
}


static const JNINativeMethod gMethods[] = {
    {"nativeCreateSubWallet", "(JILjava/lang/String;ILjava/lang/String;ZJ)J", (void*)nativeCreateSubWallet},
    {"nativeRecoverSubWallet", "(JILjava/lang/String;ILjava/lang/String;ZIJ)J", (void*)nativeRecoverSubWallet},
    {"nativeDestroyWallet", "(JJ)V", (void*)nativeDestroyWallet},
    {"nativeGetPublicKey", "(J)Ljava/lang/String;", (void*)nativeGetPublicKey},
    {"nativeSign", "(JLjava/lang/String;Ljava/lang/String;)Ljava/lang/String;", (void*)nativeSign},
    {"nativeCheckSign", "(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", (void*)nativeCheckSign},
    {"nativeDeriveIdAndKeyForPurpose", "(JIILjava/lang/String;Lcom/elastos/spvcore/IMasterWallet$IDKEY;)Z", (void*)nativeDeriveIdAndKeyForPurpose},
};

jint register_elastos_spv_IMasterWallet(JNIEnv *env)
{
    return jniRegisterNativeMethods(env, "com/elastos/spvcore/IMasterWallet",
        gMethods, NELEM(gMethods));
}