
Pod::Spec.new do |s|
  s.name         = "RNCCameraroll"
  s.version      = "1.0.0"
  s.summary      = "RNCCameraroll"
  s.description  = <<-DESC
                  RNCCameraroll
                   DESC
  s.homepage     = "https://github.com/react-native-community/react-native-cameraroll"
  s.license      = "MIT"
  # s.license      = { :type => "MIT", :file => "FILE_LICENSE" }
  s.author             = { "author" => "author@domain.cn" }
  s.platform     = :ios, "7.0"
  s.source       = { :git => "https://github.com/react-native-community/react-native-cameraroll.git", :tag => "master" }
  s.source_files  = "RNCCameraroll/**/*.{h,m}"
  s.requires_arc = true


  s.dependency "React-Core"
  #s.dependency "others"

end

  
