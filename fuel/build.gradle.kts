dependencies {
    api(Result.dependency)
    //api(KotlinX.Coroutines.jvm)

    testImplementation(project(Fuel.Test.name))
    testImplementation(Json.dependency)
}
