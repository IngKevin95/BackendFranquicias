output "app_public_ip" {
  value = aws_eip.app.public_ip
}

output "app_url" {
  value = "http://${aws_eip.app.public_ip}:8080"
}

output "db_endpoint" {
  value = aws_db_instance.postgres.address
}

output "ssh_command" {
  value = "ssh ec2-user@${aws_eip.app.public_ip}"
}
